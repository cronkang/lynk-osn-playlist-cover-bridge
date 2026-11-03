package dev.lynk.usbprobe;
import android.app.*;
import android.content.*;
import android.os.*;

/** Separate process holds original references and recovers on main-process Binder death. */
public final class RecoveryService extends Service {
 private IBinder registry,original,proxy,life;private IBinder.DeathRecipient death;private final Handler timer=new Handler(Looper.getMainLooper());private final Runnable timeout=()->{Support.log(this,"RECOVERY 独立超时恢复触发");new Thread(()->restore(),"USB-timeout-recovery").start();};
 private synchronized boolean restore(){try{if(registry!=null&&original!=null&&Support.same(Support.original(registry),proxy)){Support.add(registry,original);if(!Support.same(Support.original(registry),original))throw new IllegalStateException("恢复回读不匹配");Support.log(this,"RECOVERY 原服务恢复，复查=true");}else Support.log(this,"RECOVERY 未持有当前服务表项，不覆盖其它服务引用");}catch(Exception e){Support.log(this,"RECOVERY 恢复失败："+e);return false;}disarm();stopForeground(true);stopSelf();return true;}
 private synchronized void disarm(){timer.removeCallbacks(timeout);if(life!=null&&death!=null)try{life.unlinkToDeath(death,0);}catch(Exception ignored){}registry=original=proxy=life=null;death=null;}
 private final Binder binder=new Binder(){protected boolean onTransact(int code,Parcel d,Parcel r,int flags)throws RemoteException{
  if(code==INTERFACE_TRANSACTION){if(r!=null)r.writeString(Support.REC);return true;}d.enforceInterface(Support.REC);
  if(code==1){synchronized(RecoveryService.this){disarm();registry=d.readStrongBinder();original=d.readStrongBinder();proxy=d.readStrongBinder();life=d.readStrongBinder();death=()->{Support.log(RecoveryService.this,"RECOVERY 主进程死亡，尝试恢复");new Thread(()->restore(),"USB-recovery").start();};life.linkToDeath(death,0);timer.postDelayed(timeout,185000);}Support.log(RecoveryService.this,"RECOVERY 已保存原服务引用并监听主进程");}
  else if(code==4){boolean ok=restore();if(r!=null){r.writeNoException();r.writeInt(ok?1:0);}return true;}else if(code==2){restore();}else if(code==3){disarm();stopForeground(true);stopSelf();}else return super.onTransact(code,d,r,flags);
  if(r!=null)r.writeNoException();return true;
 }};
 public void onCreate(){super.onCreate();Support.foreground(this,1202,"恢复守护已启动；请勿强行停止整包");}
 public IBinder onBind(Intent i){return binder;}
 public int onStartCommand(Intent i,int f,int id){Support.foreground(this,1202,"恢复守护已启动；请勿强行停止整包");return START_NOT_STICKY;}
}
