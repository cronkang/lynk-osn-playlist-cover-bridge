package dev.lynk.usbprobe;
import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class Support {
 static final String REG="com.geely.lib.oneosapi.IServiceManager", MEDIA="com.geely.lib.oneosapi.mediacenter.IMediaCenter", MUSIC="com.geely.lib.oneosapi.mediacenter.IMusicManager", STATE="com.geely.lib.oneosapi.mediacenter.listener.IMediaStateListener", LIST="com.geely.lib.oneosapi.mediacenter.callback.IMusicListCallback", REC="dev.lynk.usbprobe.Recovery";
 static void log(Context c,String s){String line=new SimpleDateFormat("HH:mm:ss.SSS",Locale.ROOT).format(new Date())+" ["+android.os.Process.myPid()+"] "+s+"\n";android.util.Log.i("UsbProbe",s);try(FileOutputStream f=new FileOutputStream(new File(c.getFilesDir(),"diagnostics.txt"),true)){f.write(line.getBytes("UTF-8"));}catch(Exception ignored){}}
 static String logs(Context c){try{ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(new File(c.getFilesDir(),"diagnostics.txt"))){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}String s=out.toString("UTF-8");return s.length()>60000?s.substring(s.length()-60000):s;}catch(Exception e){return "日志尚未生成";}}
 interface Writer{void write(Parcel p)throws Exception;}
 static Parcel call(IBinder b,String descriptor,int code,Writer writer)throws Exception{if(b==null)throw new IllegalStateException("Binder missing: "+descriptor);Parcel d=Parcel.obtain(),r=Parcel.obtain();try{d.writeInterfaceToken(descriptor);if(writer!=null)writer.write(d);if(!b.transact(code,d,r,0))throw new IllegalStateException("transact=false: "+descriptor+" #"+code);r.readException();return r;}catch(Exception e){r.recycle();throw e;}finally{d.recycle();}}
 static IBinder original(IBinder registry)throws Exception{Parcel r=call(registry,REG,Contracts.code("registry","getService"),d->d.writeInt(3));try{return r.readStrongBinder();}finally{r.recycle();}}
 static void add(IBinder reg,IBinder binder)throws Exception{Parcel r=call(reg,REG,Contracts.code("registry","addService"),d->{d.writeInt(3);d.writeStrongBinder(binder);});r.recycle();}
 static boolean same(IBinder a,IBinder b){return a!=null&&b!=null&&a.equals(b);}
 static void foreground(Service s,int id,String text){NotificationManager nm=(NotificationManager)s.getSystemService(Context.NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel("usbprobe","USB 仪表验证",NotificationManager.IMPORTANCE_LOW));Intent stop=new Intent(s,ProbeService.class).setAction("restore");PendingIntent pi=PendingIntent.getService(s,2,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent open=PendingIntent.getActivity(s,1,new Intent(s,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification n=new Notification.Builder(s,"usbprobe").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("USB 仪表验证").setContentText(text).setContentIntent(open).addAction(new Notification.Action.Builder(null,"恢复原车服务",pi).build()).setOngoing(true).build();s.startForeground(id,n);}
}
