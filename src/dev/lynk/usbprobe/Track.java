package dev.lynk.usbprobe;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Wire layout matched to the actual-car MediaData Parcelable, no OEM classes bundled. */
final class Track implements Parcelable {
 String id,artist,name,uri,art,album;long duration;int source=1;Bitmap bitmap;
 Track(){}
 public int describeContents(){return 0;}
 public void writeToParcel(Parcel p,int flags){p.writeString(id);p.writeString(artist);p.writeString(name);p.writeLong(duration);p.writeString(uri);p.writeParcelable(bitmap,flags);p.writeString(art);p.writeString(album);p.writeInt(source);p.writeInt(0);p.writeInt(0);p.writeInt(0);p.writeInt(0);p.writeInt(0);p.writeInt(0);}
 static Track read(Parcel p){Track t=new Track();t.id=p.readString();t.artist=p.readString();t.name=p.readString();t.duration=p.readLong();t.uri=p.readString();t.bitmap=p.readParcelable(Bitmap.class.getClassLoader());t.art=p.readString();t.album=p.readString();t.source=p.readInt();for(int i=0;i<6;i++)p.readInt();return t;}
 static List<Track> demo(Context c)throws Exception{
  List<Track> list=new ArrayList<>();File dir=c.getExternalFilesDir("covers");if(dir==null)throw new IOException("外部应用目录不可用，不能提供跨应用可读 USB 封面");dir.mkdirs();
  int[] colors={0xff28699b,0xffb34c60,0xff578246,0xff8855aa,0xffb38426};
  for(int i=0;i<5;i++){Track t=new Track();t.id="usb-probe-"+(i+1);t.artist="Scott Joplin · 公版 MIDI";t.name=new String[]{"Bethena","The Entertainer","Magnetic Rag","Maple Leaf Rag","Original Rags"}[i];t.album="USB 五封面验证";t.duration=180000;File audio=new File(c.getFilesDir(),"track-"+(i+1)+".mid");try(InputStream in=c.getAssets().open(audio.getName());FileOutputStream out=new FileOutputStream(audio)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}t.uri=audio.getAbsolutePath();t.id=t.uri;t.bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(t.bitmap);canvas.drawColor(colors[i]);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(52);canvas.drawText(""+(i+1),48,66,paint);File f=new File(dir,"cover-"+(i+1)+".png");try(FileOutputStream out=new FileOutputStream(f)){if(!t.bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG failed");}t.art=f.getAbsolutePath();list.add(t);}
  return list;
 }
 static ParcelFileDescriptor json(Context c,List<Track> tracks)throws Exception{
  JSONArray a=new JSONArray();for(Track t:tracks){JSONObject o=new JSONObject();o.put("id",t.id);o.put("path",t.uri);o.put("name",t.name);o.put("title",t.name);o.put("artist",t.artist);o.put("album",t.album);o.put("duration",t.duration);o.put("albumArt",t.art);a.put(o);}
  File f=File.createTempFile("queue-",".json",c.getCacheDir());try(FileOutputStream out=new FileOutputStream(f)){out.write(a.toString().getBytes("UTF-8"));}ParcelFileDescriptor fd=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);f.delete();return fd;
 }
}
