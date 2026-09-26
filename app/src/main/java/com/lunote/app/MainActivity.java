package com.lunote.app;

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
  static final int CORAL=Color.rgb(185,74,61), BG=Color.rgb(246,240,231), PAPER=Color.rgb(255,250,243), TEXT=Color.rgb(36,32,31), MUTED=Color.rgb(116,106,100);
  Db db; ArrayList<Note> notes=new ArrayList<>(); NotesAdapter adapter; EditText search; TextView count; String collection="all"; boolean showArchive=false; HashMap<String,TextView> chips=new HashMap<>();

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
    if(android.os.Build.VERSION.SDK_INT>=23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    db=new Db(this); buildUi(); loadNotes();
  }

  void buildUi(){
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(dp(18),dp(18),dp(18),dp(16));
    LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
    TextView logo=tv("L",22,Color.WHITE,true); logo.setGravity(Gravity.CENTER); logo.setBackground(round(TEXT,14)); head.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
    LinearLayout titles=new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); titles.setPadding(dp(12),0,0,0); titles.addView(tv("Lunote",26,TEXT,true)); titles.addView(tv("Tu libreta privada",12,MUTED,false)); head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
    count=tv("0 notas",12,MUTED,true); head.addView(count); root.addView(head,new LinearLayout.LayoutParams(-1,dp(60)));

    LinearLayout prompt=new LinearLayout(this); prompt.setGravity(Gravity.CENTER_VERTICAL); prompt.setPadding(dp(16),0,dp(16),0); prompt.setBackground(strokeRound(Color.rgb(241,218,207),Color.rgb(229,198,183),18,1));
    prompt.addView(tv("✦  Captura lo que te ronda la cabeza.",14,TEXT,true));
    LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(50)); pp.setMargins(0,dp(12),0,dp(12)); root.addView(prompt,pp);

    search=new EditText(this); search.setHint("Busca una idea, palabra o pendiente"); search.setTextSize(15); search.setSingleLine(true); search.setPadding(dp(15),0,dp(15),0); search.setBackground(strokeRound(PAPER,Color.rgb(222,210,197),16,1));
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52)); sp.setMargins(0,0,0,dp(10)); root.addView(search,sp);
    search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){loadNotes();} public void afterTextChanged(Editable e){}});

    LinearLayout filters=new LinearLayout(this); filters.setOrientation(LinearLayout.HORIZONTAL);
    filters.addView(chip("Todas","all")); filters.addView(chip("Ideas","Ideas")); filters.addView(chip("Personal","Personal")); filters.addView(chip("Trabajo","Trabajo"));
    TextView archive=chip("Archivo","archive"); filters.addView(archive);
    HorizontalScrollView scroll=new HorizontalScrollView(this); scroll.setHorizontalScrollBarEnabled(false); scroll.addView(filters);
    LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(44)); fp.setMargins(0,0,0,dp(10)); root.addView(scroll,fp);

    FrameLayout frame=new FrameLayout(this); ListView list=new ListView(this); list.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)); list.setDividerHeight(dp(10)); list.setClipToPadding(false); list.setPadding(0,0,0,dp(8));
    adapter=new NotesAdapter(); list.setAdapter(adapter);
    TextView empty=tv("Una página en blanco.\nToca “Nueva nota” para empezar.",15,MUTED,false); empty.setGravity(Gravity.CENTER); frame.addView(list,new FrameLayout.LayoutParams(-1,-1)); frame.addView(empty,new FrameLayout.LayoutParams(-1,-1)); list.setEmptyView(empty);
    root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
    list.setOnItemClickListener((p,v,pos,id)->openEditor(notes.get(pos)));
    list.setOnItemLongClickListener((p,v,pos,id)->{showMenu(v,notes.get(pos));return true;});

    TextView add=tv("＋  Nueva nota",16,Color.WHITE,true); add.setGravity(Gravity.CENTER); add.setBackground(round(CORAL,17)); add.setElevation(dp(5)); add.setOnClickListener(v->openEditor(null));
    LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(56)); ap.setMargins(0,dp(10),0,0); root.addView(add,ap);
    setContentView(root); styleChips();
  }

  TextView chip(String label,String value){
    TextView v=tv(label,13,TEXT,true); v.setGravity(Gravity.CENTER); v.setPadding(dp(14),0,dp(14),0);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(36)); lp.setMargins(0,0,dp(7),0); v.setLayoutParams(lp);
    v.setOnClickListener(x->{ if("archive".equals(value)){showArchive=!showArchive;} else {collection=value;showArchive=false;} styleChips();loadNotes();});
    chips.put(value,v); return v;
  }
  void styleChips(){
    for(Map.Entry<String,TextView> e:chips.entrySet()){
      boolean on=("archive".equals(e.getKey())?showArchive:(!showArchive && e.getKey().equals(collection)));
      e.getValue().setTextColor(on?Color.WHITE:TEXT);
      e.getValue().setBackground(round(on?TEXT:Color.rgb(233,221,208),18));
    }
  }
  void loadNotes(){
    if(adapter==null)return; notes.clear(); notes.addAll(db.list(search.getText().toString(),collection,showArchive)); adapter.notifyDataSetChanged();
    count.setText(notes.size()==1?"1 nota":notes.size()+" notas");
  }

  void openEditor(Note note){
    LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(4),dp(6),dp(4),0);
    Spinner spinner=new Spinner(this); String[] labels={"Ideas","Personal","Trabajo"}; ArrayAdapter<String> sa=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels); spinner.setAdapter(sa);
    if(note!=null){for(int i=0;i<labels.length;i++)if(labels[i].equals(note.collection))spinner.setSelection(i);}
    EditText title=new EditText(this); title.setHint("Ponle un título"); title.setSingleLine(true); title.setTextSize(19); title.setText(note==null?"":note.title);
    EditText body=new EditText(this); body.setHint("Escribe sin ordenar. Guarda la idea primero."); body.setGravity(Gravity.TOP); body.setMinLines(9); body.setTextSize(16); body.setText(note==null?"":note.body);
    box.addView(spinner,new LinearLayout.LayoutParams(-1,dp(52))); box.addView(title,new LinearLayout.LayoutParams(-1,dp(56))); box.addView(body,new LinearLayout.LayoutParams(-1,dp(250)));
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle(note==null?"Nueva nota":"Editar nota").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();
    dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
      String t=title.getText().toString().trim(), bb=body.getText().toString().trim();
      if(t.isEmpty()&&bb.isEmpty()){Toast.makeText(this,"Escribe algo antes de guardar",Toast.LENGTH_SHORT).show();return;}
      Note n=note==null?new Note():note; n.title=t.isEmpty()?"Sin título":t; n.body=bb; n.collection=String.valueOf(spinner.getSelectedItem()); n.updated=System.currentTimeMillis(); db.save(n); dialog.dismiss(); loadNotes();
    }));
    dialog.show();
  }

  void showMenu(View anchor,Note n){
    PopupMenu m=new PopupMenu(this,anchor);
    m.getMenu().add(0,1,0,n.favorite==1?"Quitar de favoritas":"Marcar favorita");
    m.getMenu().add(0,2,1,n.archived==1?"Sacar del archivo":"Archivar");
    m.getMenu().add(0,3,2,"Compartir");
    m.getMenu().add(0,4,3,"Eliminar");
    m.setOnMenuItemClickListener(i->{
      if(i.getItemId()==1){db.toggle(n.id,"favorite");loadNotes();return true;}
      if(i.getItemId()==2){db.toggle(n.id,"archived");loadNotes();return true;}
      if(i.getItemId()==3){Intent s=new Intent(Intent.ACTION_SEND);s.setType("text/plain");s.putExtra(Intent.EXTRA_TEXT,n.title+"\n\n"+n.body);startActivity(Intent.createChooser(s,"Compartir nota"));return true;}
      if(i.getItemId()==4){new AlertDialog.Builder(this).setTitle("Eliminar nota").setMessage("Esta acción no se puede deshacer.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{db.delete(n.id);loadNotes();}).show();return true;}
      return false;
    }); m.show();
  }

  TextView tv(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
  GradientDrawable strokeRound(int color,int stroke,int r,int w){GradientDrawable g=round(color,r);g.setStroke(dp(w),stroke);return g;}
  int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}

  class NotesAdapter extends BaseAdapter{
    SimpleDateFormat fmt=new SimpleDateFormat("dd MMM · HH:mm",new Locale("es","MX"));
    public int getCount(){return notes.size();} public Object getItem(int p){return notes.get(p);} public long getItemId(int p){return notes.get(p).id;}
    public View getView(int pos,View cv,ViewGroup parent){
      LinearLayout card; TextView title,body,meta;
      if(cv==null){
        card=new LinearLayout(MainActivity.this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(16),dp(14),dp(16),dp(14)); card.setBackground(strokeRound(PAPER,Color.rgb(222,210,197),19,1));
        title=tv("",17,TEXT,true); body=tv("",14,MUTED,false); body.setMaxLines(3); body.setEllipsize(TextUtils.TruncateAt.END); body.setPadding(0,dp(7),0,dp(9)); meta=tv("",11,MUTED,false);
        card.addView(title); card.addView(body); card.addView(meta); card.setTag(new TextView[]{title,body,meta});
      } else {card=(LinearLayout)cv;TextView[] a=(TextView[])card.getTag();title=a[0];body=a[1];meta=a[2];}
      Note n=notes.get(pos); String flags=n.favorite==1?"★ ":""; title.setText(flags+n.title); body.setText(n.body.isEmpty()?"Nota sin contenido":n.body); meta.setText(n.collection+" · "+fmt.format(new Date(n.updated))+" · Mantén pulsado");
      return card;
    }
  }

  static class Note{long id;String title="",body="",collection="Ideas";int favorite,archived;long updated;}
  static class Db extends SQLiteOpenHelper{
    Db(Context c){super(c,"lunote.db",null,1);}
    public void onCreate(SQLiteDatabase d){
      d.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,body TEXT NOT NULL,collection TEXT NOT NULL DEFAULT 'Ideas',favorite INTEGER NOT NULL DEFAULT 0,archived INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL)");
      ContentValues v=new ContentValues(); v.put("title","Bienvenido a Lunote"); v.put("body","Escribe ideas, ordénalas por colección, marca favoritas y archiva lo que ya terminaste."); v.put("collection","Ideas"); v.put("updated_at",System.currentTimeMillis()); d.insert("notes",null,v);
    }
    public void onUpgrade(SQLiteDatabase d,int o,int n){}
    ArrayList<Note> list(String q,String col,boolean archivedOnly){
      ArrayList<Note> out=new ArrayList<>(); ArrayList<String> args=new ArrayList<>(),where=new ArrayList<>();
      where.add("archived=?"); args.add(archivedOnly?"1":"0");
      if(q!=null&&!q.trim().isEmpty()){where.add("(title LIKE ? OR body LIKE ?)");args.add("%"+q.trim()+"%");args.add("%"+q.trim()+"%");}
      if(!"all".equals(col)&&!archivedOnly){where.add("collection=?");args.add(col);}
      String sql="SELECT id,title,body,collection,favorite,archived,updated_at FROM notes WHERE "+TextUtils.join(" AND ",where)+" ORDER BY favorite DESC,updated_at DESC";
      Cursor c=getReadableDatabase().rawQuery(sql,args.toArray(new String[0]));
      while(c.moveToNext()){Note n=new Note();n.id=c.getLong(0);n.title=c.getString(1);n.body=c.getString(2);n.collection=c.getString(3);n.favorite=c.getInt(4);n.archived=c.getInt(5);n.updated=c.getLong(6);out.add(n);} c.close(); return out;
    }
    void save(Note n){ContentValues v=new ContentValues();v.put("title",n.title);v.put("body",n.body);v.put("collection",n.collection);v.put("favorite",n.favorite);v.put("archived",n.archived);v.put("updated_at",n.updated);if(n.id==0)n.id=getWritableDatabase().insert("notes",null,v);else getWritableDatabase().update("notes",v,"id=?",new String[]{String.valueOf(n.id)});}
    void toggle(long id,String col){if(!"favorite".equals(col)&&!"archived".equals(col))return;getWritableDatabase().execSQL("UPDATE notes SET "+col+"=CASE "+col+" WHEN 1 THEN 0 ELSE 1 END,updated_at=? WHERE id=?",new Object[]{System.currentTimeMillis(),id});}
    void delete(long id){getWritableDatabase().delete("notes","id=?",new String[]{String.valueOf(id)});}
  }
}
