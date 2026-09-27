package com.trazo.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  static final int ACCENT=Color.rgb(215,74,42), INK=Color.rgb(42,39,35), BG=Color.rgb(244,239,223), PAPER=Color.rgb(251,247,236), MUTED=Color.rgb(116,107,95), BORDER=Color.rgb(207,195,171);
  Db db; ArrayList<Note> notes=new ArrayList<>(); NotesAdapter adapter; EditText search; TextView count; String filter="all"; HashMap<String,TextView> chips=new HashMap<>();

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
    if(android.os.Build.VERSION.SDK_INT>=23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    db=new Db(this); buildUi(); loadNotes();
  }

  void buildUi(){
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(dp(18),dp(16),dp(18),dp(14));

    LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
    TextView logo=tv("T",18,BG,true); logo.setGravity(Gravity.CENTER); logo.setBackground(round(INK,4)); head.addView(logo,new LinearLayout.LayoutParams(dp(46),dp(46)));
    LinearLayout titles=new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); titles.setPadding(dp(12),0,0,0); titles.addView(tv("Trazo",29,INK,true)); titles.addView(tv("Deja algo por escrito.",12,MUTED,false)); head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
    count=tv("0",12,ACCENT,true); count.setGravity(Gravity.CENTER); count.setBackground(strokeRound(PAPER,BORDER,5,1)); head.addView(count,new LinearLayout.LayoutParams(dp(48),dp(36)));
    root.addView(head,new LinearLayout.LayoutParams(-1,dp(60)));

    TextView date=tv(new SimpleDateFormat("EEEE · dd MMMM",new Locale("es","MX")).format(new Date()).toUpperCase(),11,MUTED,true);
    date.setLetterSpacing(.08f);
    LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(-1,dp(34)); dpv.setMargins(0,dp(8),0,dp(5)); root.addView(date,dpv);

    search=new EditText(this); search.setHint("Buscar en mis trazos"); search.setTextSize(15); search.setSingleLine(true); search.setPadding(dp(14),0,dp(14),0); search.setTextColor(INK); search.setHintTextColor(Color.rgb(150,140,126)); search.setBackground(strokeRound(PAPER,BORDER,7,1));
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(50)); sp.setMargins(0,0,0,dp(11)); root.addView(search,sp);
    search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){loadNotes();} public void afterTextChanged(Editable e){}});

    LinearLayout filters=new LinearLayout(this); filters.setOrientation(LinearLayout.HORIZONTAL);
    filters.addView(chip("Todos","all")); filters.addView(chip("Apunte","note")); filters.addView(chip("Idea","idea")); filters.addView(chip("Pendiente","todo"));
    HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false); hs.addView(filters);
    LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(43)); fp.setMargins(0,0,0,dp(9)); root.addView(hs,fp);

    FrameLayout frame=new FrameLayout(this);
    ListView list=new ListView(this); list.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)); list.setDividerHeight(dp(9)); list.setClipToPadding(false); list.setPadding(0,0,0,dp(8));
    adapter=new NotesAdapter(); list.setAdapter(adapter);
    TextView empty=tv("Todavía no hay trazos.\nEscribe el primero.",15,MUTED,false); empty.setGravity(Gravity.CENTER);
    frame.addView(list,new FrameLayout.LayoutParams(-1,-1)); frame.addView(empty,new FrameLayout.LayoutParams(-1,-1)); list.setEmptyView(empty);
    root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
    list.setOnItemClickListener((p,v,pos,id)->openEditor(notes.get(pos)));
    list.setOnItemLongClickListener((p,v,pos,id)->{showMenu(v,notes.get(pos));return true;});

    TextView add=tv("＋  Nuevo trazo",16,Color.WHITE,true); add.setGravity(Gravity.CENTER); add.setBackground(round(ACCENT,6)); add.setElevation(dp(3)); add.setOnClickListener(v->openEditor(null));
    LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(54)); ap.setMargins(0,dp(8),0,0); root.addView(add,ap);
    setContentView(root); styleChips();
  }

  TextView chip(String label,String value){
    TextView v=tv(label,13,INK,true); v.setGravity(Gravity.CENTER); v.setPadding(dp(14),0,dp(14),0);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(35)); lp.setMargins(0,0,dp(7),0); v.setLayoutParams(lp);
    v.setOnClickListener(x->{filter=value;styleChips();loadNotes();}); chips.put(value,v); return v;
  }
  void styleChips(){for(Map.Entry<String,TextView> e:chips.entrySet()){boolean on=e.getKey().equals(filter);e.getValue().setTextColor(on?Color.WHITE:INK);e.getValue().setBackground(strokeRound(on?INK:PAPER,on?INK:BORDER,5,1));}}
  void loadNotes(){if(adapter==null)return;notes.clear();notes.addAll(db.list(search.getText().toString(),filter));adapter.notifyDataSetChanged();count.setText(String.valueOf(notes.size()));}

  void openEditor(Note note){
    LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(4),dp(4),dp(4),0);
    Spinner kind=new Spinner(this); String[] kinds={"note","idea","todo"}; String[] labels={"Apunte","Idea","Pendiente"}; ArrayAdapter<String> sa=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);kind.setAdapter(sa);
    if(note!=null){for(int i=0;i<kinds.length;i++)if(kinds[i].equals(note.kind))kind.setSelection(i);}
    EditText title=new EditText(this); title.setHint("Título"); title.setSingleLine(true); title.setTextSize(19); title.setText(note==null?"":note.title);
    EditText body=new EditText(this); body.setHint("Escribe lo que no quieres perder..."); body.setGravity(Gravity.TOP); body.setMinLines(9); body.setTextSize(16); body.setText(note==null?"":note.body);
    box.addView(kind,new LinearLayout.LayoutParams(-1,dp(50))); box.addView(title,new LinearLayout.LayoutParams(-1,dp(56))); box.addView(body,new LinearLayout.LayoutParams(-1,dp(250)));
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle(note==null?"Nuevo trazo":"Editar trazo").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();
    dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
      String t=title.getText().toString().trim(),bb=body.getText().toString().trim();
      if(t.isEmpty()&&bb.isEmpty()){Toast.makeText(this,"Escribe algo antes de guardar",Toast.LENGTH_SHORT).show();return;}
      Note n=note==null?new Note():note; n.title=t.isEmpty()?"Sin título":t; n.body=bb; n.kind=kinds[kind.getSelectedItemPosition()]; n.updated=System.currentTimeMillis(); db.save(n); dialog.dismiss(); loadNotes();
    }));
    dialog.show();
  }

  void showMenu(View anchor,Note n){
    PopupMenu m=new PopupMenu(this,anchor);
    m.getMenu().add(0,1,0,n.pinned==1?"Desfijar":"Fijar arriba");
    m.getMenu().add(0,2,1,"Compartir");
    m.getMenu().add(0,3,2,"Eliminar");
    m.setOnMenuItemClickListener(i->{
      if(i.getItemId()==1){db.togglePin(n.id);loadNotes();return true;}
      if(i.getItemId()==2){Intent s=new Intent(Intent.ACTION_SEND);s.setType("text/plain");s.putExtra(Intent.EXTRA_TEXT,n.title+"\n\n"+n.body);startActivity(Intent.createChooser(s,"Compartir trazo"));return true;}
      if(i.getItemId()==3){new AlertDialog.Builder(this).setTitle("Eliminar trazo").setMessage("Esta acción no se puede deshacer.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{db.delete(n.id);loadNotes();}).show();return true;}
      return false;
    });m.show();
  }

  TextView tv(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
  GradientDrawable strokeRound(int color,int stroke,int r,int w){GradientDrawable g=round(color,r);g.setStroke(dp(w),stroke);return g;}
  int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
  String kindLabel(String k){if("idea".equals(k))return "IDEA";if("todo".equals(k))return "PENDIENTE";return "APUNTE";}

  class NotesAdapter extends BaseAdapter{
    SimpleDateFormat fmt=new SimpleDateFormat("dd MMM · HH:mm",new Locale("es","MX"));
    public int getCount(){return notes.size();} public Object getItem(int p){return notes.get(p);} public long getItemId(int p){return notes.get(p).id;}
    public View getView(int pos,View cv,ViewGroup parent){
      LinearLayout card;TextView tag,title,body,meta;
      if(cv==null){
        card=new LinearLayout(MainActivity.this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(13),dp(16),dp(13));card.setBackground(strokeRound(PAPER,BORDER,7,1));
        tag=tv("",10,ACCENT,true);title=tv("",18,INK,true);title.setPadding(0,dp(7),0,0);body=tv("",14,MUTED,false);body.setMaxLines(3);body.setEllipsize(TextUtils.TruncateAt.END);body.setPadding(0,dp(6),0,dp(9));meta=tv("",11,MUTED,false);
        card.addView(tag);card.addView(title);card.addView(body);card.addView(meta);card.setTag(new TextView[]{tag,title,body,meta});
      } else {card=(LinearLayout)cv;TextView[] a=(TextView[])card.getTag();tag=a[0];title=a[1];body=a[2];meta=a[3];}
      Note n=notes.get(pos);tag.setText(kindLabel(n.kind));title.setText((n.pinned==1?"● ":"")+n.title);body.setText(n.body.isEmpty()?"Sin contenido":n.body);meta.setText(fmt.format(new Date(n.updated))+" · Mantén pulsado");return card;
    }
  }

  static class Note{long id;String title="",body="",kind="note";int pinned;long updated;}
  static class Db extends SQLiteOpenHelper{
    Db(Context c){super(c,"trazo.db",null,1);}
    public void onCreate(SQLiteDatabase d){
      d.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,body TEXT NOT NULL,kind TEXT NOT NULL DEFAULT 'note',pinned INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL)");
      ContentValues v=new ContentValues();v.put("title","Bienvenido a Trazo");v.put("body","Captura ideas, apuntes y pendientes. Todo se guarda localmente en este teléfono.");v.put("kind","note");v.put("updated_at",System.currentTimeMillis());d.insert("notes",null,v);
    }
    public void onUpgrade(SQLiteDatabase d,int o,int n){}
    ArrayList<Note> list(String q,String f){
      ArrayList<Note> out=new ArrayList<>();ArrayList<String> args=new ArrayList<>(),where=new ArrayList<>();
      if(q!=null&&!q.trim().isEmpty()){where.add("(title LIKE ? OR body LIKE ?)");args.add("%"+q.trim()+"%");args.add("%"+q.trim()+"%");}
      if(!"all".equals(f)){where.add("kind=?");args.add(f);}
      String sql="SELECT id,title,body,kind,pinned,updated_at FROM notes"+(where.isEmpty()?"":" WHERE "+TextUtils.join(" AND ",where))+" ORDER BY pinned DESC,updated_at DESC";
      Cursor c=getReadableDatabase().rawQuery(sql,args.toArray(new String[0]));
      while(c.moveToNext()){Note n=new Note();n.id=c.getLong(0);n.title=c.getString(1);n.body=c.getString(2);n.kind=c.getString(3);n.pinned=c.getInt(4);n.updated=c.getLong(5);out.add(n);}c.close();return out;
    }
    void save(Note n){ContentValues v=new ContentValues();v.put("title",n.title);v.put("body",n.body);v.put("kind",n.kind);v.put("pinned",n.pinned);v.put("updated_at",n.updated);if(n.id==0)n.id=getWritableDatabase().insert("notes",null,v);else getWritableDatabase().update("notes",v,"id=?",new String[]{String.valueOf(n.id)});}
    void togglePin(long id){getWritableDatabase().execSQL("UPDATE notes SET pinned=CASE pinned WHEN 1 THEN 0 ELSE 1 END,updated_at=? WHERE id=?",new Object[]{System.currentTimeMillis(),id});}
    void delete(long id){getWritableDatabase().delete("notes","id=?",new String[]{String.valueOf(id)});}
  }
}
