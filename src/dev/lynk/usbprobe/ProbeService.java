package dev.lynk.usbprobe;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.media.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ProbeService extends Service {
 private HandlerThread thread;private Handler work;private final Handler main=new Handler(Looper.getMainLooper());
 private IBinder registry,original,music,watcher;private final Binder life=new Binder();
 private volatile boolean active;private boolean apiBound,recoveryBound,enablePending,inspected,recoveryOnly;private long deadline;private volatile int index=0;private int appSource;
 private ResultReceiver uninstallResult;
 private MediaPlayer player;private volatile boolean playing;private volatile long position;private AudioManager audio;private AudioFocusRequest focus;private boolean usbObserved;
 private List<Track> tracks;private MediaProxy proxy;
 private final CopyOnWriteArrayList<Listener> listeners=new CopyOnWriteArrayList<>();
 private final ServiceConnection apiConnection=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder b){work.post(()->{try{if(!Support.REG.equals(b.getInterfaceDescriptor()))throw new IllegalStateException("Unexpected registry descriptor");registry=b;log("[A2] OneOS 已连接，descriptor="+b.getInterfaceDescriptor());diagnose();attemptEnable();}catch(Exception e){fail("连接检查",e);}});}public void onServiceDisconnected(ComponentName n){registry=null;log("OneOS 服务连接断开");}};
 private final ServiceConnection recoveryConnection=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder b){work.post(()->{watcher=b;log("[R1] 独立恢复进程已连接");if(recoveryOnly){recoveryOnly=false;restore();}else attemptEnable();});}public void onServiceDisconnected(ComponentName n){watcher=null;log("恢复进程连接断开，停止测试");work.post(()->restore());}};
 public void onCreate(){super.onCreate();Support.foreground(this,1201,"未启用；等待手动检查或测试");thread=new HandlerThread("USB-Probe");thread.start();work=new Handler(thread.getLooper());try{tracks=Track.demo(this);proxy=new MediaProxy();log("[A0] 五首测试数据生成完成；封面目录="+tracks.get(0).art);}catch(Exception e){fail("准备数据",e);}}
 public IBinder onBind(Intent i){return null;}
 public int onStartCommand(Intent i,int flags,int id){if(i==null)return START_NOT_STICKY;String a=i.getAction();work.post(()->{log("用户动作="+a);try{if("restore".equals(a)||"uninstall".equals(a)){if("uninstall".equals(a))uninstallResult=i.getParcelableExtra("result");enablePending=false;if(watcher==null&&original==null){recoveryOnly=true;connectRecovery();}else restore();return;}if("pauseAudio".equals(a)){pauseAudio();return;}if("resumeAudio".equals(a)){resumeAudio();return;}if("select".equals(a)){select(i.getIntExtra("index",1));return;}if(active){log("测试已启用，剩余秒数="+Math.max(0,(deadline-SystemClock.elapsedRealtime())/1000));return;}enablePending="enable".equals(a);connect();if(registry!=null){diagnose();attemptEnable();}}catch(Exception e){enablePending=false;fail("动作 "+a,e);}});return START_NOT_STICKY;}
 private void log(String s){Support.log(this,s);}
 private void fail(String stage,Throwable e){log("失败 / "+stage+" / "+android.util.Log.getStackTraceString(e));}
 private void connect(){if(!apiBound){main.post(()->{Intent i=new Intent().setClassName("com.geely.service.oneosapi","com.geely.service.oneosapi.OneOSApiService");try{apiBound=bindService(i,apiConnection,Context.BIND_AUTO_CREATE);log("[A1] bind OneOS 返回="+apiBound);}catch(Exception e){fail("bind OneOS",e);}});}}
 private void connectRecovery(){if(recoveryBound)return;main.post(()->{try{Intent i=new Intent(this,RecoveryService.class);startForegroundService(i);recoveryBound=bindService(i,recoveryConnection,Context.BIND_AUTO_CREATE);log("[R0] bind 恢复守护="+recoveryBound);}catch(Exception e){enablePending=false;fail("恢复守护",e);}});}
 private IBinder getMusic(IBinder media)throws Exception{Parcel r=Support.call(media,Support.MEDIA,Contracts.code("media","getMusicManager"),null);try{return r.readStrongBinder();}finally{r.recycle();}}
 private int query(IBinder b,String group,String desc,String method,boolean usb)throws Exception{Parcel r=Support.call(b,desc,Contracts.code(group,method),usb?d->d.writeInt(1):null);try{return r.readInt();}finally{r.recycle();}}
 private void diagnose()throws Exception{
  if(active){log("已启用时不重新采样原服务引用");return;}inspected=false;
  for(String pkg:new String[]{"com.geely.service.oneosapi","com.geely.mediacenterservice","com.geely.usbservice","com.geely.dimmenu"})try{PackageInfo p=getPackageManager().getPackageInfo(pkg,0);log("版本 "+pkg+" / "+p.versionName);}catch(Exception e){log("未发现包 "+pkg);}
  IBinder found=Support.original(registry);if(found==null||!found.isBinderAlive())throw new IllegalStateException("getService(3) 未取得存活 Binder");
  if(!Support.MEDIA.equals(found.getInterfaceDescriptor()))throw new IllegalStateException("Unexpected media descriptor "+found.getInterfaceDescriptor());
  original=found;music=getMusic(original);if(music==null||!Support.MUSIC.equals(music.getInterfaceDescriptor()))throw new IllegalStateException("USB music manager descriptor mismatch");
  int source=query(original,"media",Support.MEDIA,"getCurrentAudioSource",false);appSource=query(original,"media",Support.MEDIA,"getCurrentAppSource",false);
  log("[A3] 原媒体中心可读，当前音源="+source+"（USB=1），appSource="+appSource);
  try{Parcel r=Support.call(music,Support.MUSIC,Contracts.code("music","getCurrentMediaData"),d->d.writeInt(1));try{if(r.readInt()!=0){Track t=Track.read(r);log("[A4] 原USB当前曲="+t.name+"，source="+t.source+"，id="+t.id);}else log("[A4] 原USB当前曲为空");}finally{r.recycle();}
  r=Support.call(music,Support.MUSIC,Contracts.code("music","getUsbPlayList"),d->d.writeInt(1));try{if(r.readInt()!=0){ParcelFileDescriptor fd=ParcelFileDescriptor.CREATOR.createFromParcel(r);log("[A5] 原USB队列描述符已取得，statSize="+fd.getStatSize());fd.close();}else log("[A5] 原USB队列描述符为空");}finally{r.recycle();}
  }catch(Exception e){log("[A5] 无U盘原数据不可用，继续验证："+e);}
  inspected=true;log("只读检查结束，尚未修改任何服务引用");
 }
 private void attemptEnable(){if(!enablePending||active||!inspected||registry==null||original==null||tracks==null)return;if(watcher==null){connectRecovery();return;}try{
  appSource=1;usbObserved=false;log("[B0] 无U盘模式，原音源="+query(original,"media",Support.MEDIA,"getCurrentAudioSource",false));
  if(!Support.same(Support.original(registry),original))throw new IllegalStateException("原服务引用已变化，请重新检查");
  Parcel r=Support.call(watcher,Support.REC,1,d->{d.writeStrongBinder(registry);d.writeStrongBinder(original);d.writeStrongBinder(proxy);d.writeStrongBinder(life);});r.recycle();
  active=true;deadline=SystemClock.elapsedRealtime()+180000;Support.add(registry,proxy);
  if(!Support.same(Support.original(registry),proxy))throw new IllegalStateException("注册返回后 getService(3) 不等于本代理");
  enablePending=false;log("[B1] 服务表回读已确认本代理。还不代表仪表已读取数据。");main.post(()->Support.foreground(this,1201,"USB 五首显示验证已启用，3分钟自动恢复"));
  requestUsb();work.postDelayed(()->{if(active)try{startAudio();publish();}catch(Exception e){stopAudio();fail("内置MIDI播放，继续验证USB显示",e);}},1200);work.postDelayed(tick,500);work.postDelayed(()->{if(active&&SystemClock.elapsedRealtime()>=deadline)restore();},180100);
 }catch(Exception e){enablePending=false;fail("启用",e);restore();}}
 private final Runnable tick=new Runnable(){public void run(){if(!active)return;try{if(SystemClock.elapsedRealtime()>=deadline){restore();return;}if(!Support.same(Support.original(registry),proxy)){log("服务表已被系统更新，本测试停止覆盖");restore();return;}int observed=query(original,"media",Support.MEDIA,"getCurrentAudioSource",false);if(observed==1)syncUsbApp();log("[S2] 原服务真实音源="+observed+"，音频="+playing);if(observed==1)usbObserved=true;else if(usbObserved){log("音源离开USB，自动恢复");restore();return;}if(player!=null)position=player.getCurrentPosition();publish();}catch(Exception e){fail("心跳",e);restore();return;}if(active)work.postDelayed(this,2000);}};
 private synchronized void restore(){enablePending=false;active=false;stopAudio();work.removeCallbacks(tick);boolean restored=false;try{
  if(registry!=null&&original!=null){IBinder now=Support.original(registry);if(Support.same(now,proxy)){Support.add(registry,original);restored=Support.same(Support.original(registry),original);log("[R2] 原媒体服务恢复回读="+restored);if(!restored)throw new IllegalStateException("恢复回读不匹配");}else{restored=true;log("[R2] 当前服务不是本代理，保留现有服务引用");}}
  if(watcher!=null){Parcel r=Support.call(watcher,Support.REC,restored?3:4,null);try{if(!restored&&r.readInt()!=1)throw new IllegalStateException("独立恢复进程未确认恢复成功");}finally{r.recycle();}}
  for(Listener l:listeners){try{Parcel r=Support.call(music,Support.MUSIC,Contracts.code("music",l.multi?"addMediaStateListener":"addMusicStateListener"),d->{d.writeInt(1);d.writeStrongBinder(l.b);});r.recycle();}catch(Exception e){log("恢复监听器："+e);}}listeners.clear();
  if(uninstallResult!=null){uninstallResult.send(1,Bundle.EMPTY);uninstallResult=null;}
  main.post(()->{stopForeground(true);stopSelf();});
 }catch(Exception e){fail("恢复服务，守护仍保留原引用",e);if(uninstallResult!=null){uninstallResult.send(0,Bundle.EMPTY);uninstallResult=null;}}}
 private void select(int n){if(!active){log("尚未启用 USB 代理，不发送测试数据");return;}index=Math.floorMod(n,tracks.size());log("[C0] 测试当前项="+tracks.get(index).name);try{startAudio();}catch(Exception e){fail("播放",e);}publish();}
 private void requestUsb(){try{Parcel r=Support.call(original,Support.MEDIA,Contracts.code("media","requestAudioSource"),d->{d.writeInt(1);d.writeInt(0);});r.recycle();syncUsbApp();log("[S1] 已调用原版 requestAudioSource(USB=1,UNKNOWN=0)，以S2回读判断结果");}catch(Exception e){fail("申请USB音源",e);}}
 private void syncUsbApp(){try{int actual=query(original,"media",Support.MEDIA,"getCurrentAppSource",false);int source=query(original,"media",Support.MEDIA,"getCurrentAudioSource",false);if(source==1&&actual!=appSource){log("[S3] USB真实appSource="+actual+"，旧值="+appSource);appSource=actual;for(Listener l:listeners)l.lastIndex=-1;}}catch(Exception e){log("[S3] appSource读取失败="+e);}}
 private void stopAudio(){playing=false;position=0;if(player!=null){player.release();player=null;}if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);focus=null;}
 private void startAudio()throws Exception{stopAudio();audio=(AudioManager)getSystemService(AUDIO_SERVICE);AudioAttributes attr=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attr).setOnAudioFocusChangeListener(change->work.post(()->{log("[P2] 音频焦点变化="+change);if(change<0)pauseAudio();}),work).build();int result=audio.requestAudioFocus(focus);log("[P0] 音频焦点申请="+result);if(result!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)throw new IllegalStateException("未获得音频焦点");player=new MediaPlayer();player.setAudioAttributes(attr);player.setDataSource(tracks.get(index).uri);player.setOnCompletionListener(m->work.post(()->select(index+1)));player.setOnErrorListener((m,x,y)->{work.post(()->{playing=false;log("[P3] 播放错误="+x+","+y);publish();});return true;});player.prepare();tracks.get(index).duration=player.getDuration();player.start();playing=true;log("[P1] 已启动MIDI："+tracks.get(index).name+"，时长="+player.getDuration());}
 private void pauseAudio(){if(player!=null&&playing){player.pause();position=player.getCurrentPosition();playing=false;}publish();}
 private void resumeAudio(){if(!active)return;try{if(player==null)startAudio();else{player.start();playing=true;}publish();}catch(Exception e){fail("继续播放",e);}}
 private void callback(IBinder b,String desc,int code,Support.Writer w)throws Exception{Parcel r=Support.call(b,desc,code,w);r.recycle();}
 private void publish(){if(!active)return;for(Listener l:listeners)try{if(!l.b.isBinderAlive()){listeners.remove(l);continue;}String group=l.multi?"state":"musicState",desc=l.multi?Support.STATE:"com.geely.lib.oneosapi.mediacenter.listener.IMusicStateListener";
  if(l.lastIndex!=index||l.lastDuration!=tracks.get(index).duration){
   try(ParcelFileDescriptor fd=Track.json(this,tracks)){callback(l.b,desc,Contracts.code(group,"onUsbPlayListChanged"),d->{d.writeInt(1);if(l.multi)d.writeInt(appSource);d.writeInt(1);fd.writeToParcel(d,0);});}
   callback(l.b,desc,Contracts.code(group,"onPlayListChanged"),d->{d.writeInt(1);if(l.multi)d.writeInt(appSource);d.writeTypedList(tracks);});
   callback(l.b,desc,Contracts.code(group,"onMediaDataChanged"),d->{d.writeInt(1);if(l.multi)d.writeInt(appSource);d.writeInt(1);tracks.get(index).writeToParcel(d,0);});
   log("[D1] 列表PFD+五Bitmap及当前曲已推送，appSource="+appSource+"，index="+index+"，listener="+l.b);
   l.lastIndex=index;l.lastDuration=tracks.get(index).duration;
  }
  callback(l.b,desc,Contracts.code(group,"onPlayStateChanged"),d->{d.writeInt(1);if(l.multi)d.writeInt(appSource);d.writeInt(playing?0:1);});
  callback(l.b,desc,Contracts.code(group,"onPlayPositionChanged"),d->{d.writeInt(1);if(l.multi)d.writeInt(appSource);d.writeLong(position);d.writeLong(tracks.get(index).duration);});
  log("[D2] 状态进度回调返回，appSource="+appSource+"，playing="+playing+"，position="+position+"，duration="+tracks.get(index).duration);
 }catch(Exception e){log("回调失败："+e);}}
 private static final class Listener{final IBinder b;final boolean multi;int lastIndex=-1;long lastDuration=-1;Listener(IBinder b,boolean m){this.b=b;multi=m;}}
 private final class MediaProxy extends Binder {
  private final MusicProxy nested=new MusicProxy();MediaProxy(){attachInterface(null,Support.MEDIA);}
  protected boolean onTransact(int code,Parcel d,Parcel r,int flags)throws RemoteException{if(code==INTERFACE_TRANSACTION){r.writeString(Support.MEDIA);return true;}if(active&&code==Contracts.code("media","getMusicManager")){d.enforceInterface(Support.MEDIA);r.writeNoException();r.writeStrongBinder(nested);log("[B2] 客户端取得本代理的音乐接口");return true;}d.setDataPosition(0);return original!=null&&original.transact(code,d,r,flags);}
 }
 private final class MusicProxy extends Binder {
  MusicProxy(){attachInterface(null,Support.MUSIC);}
  protected boolean onTransact(int code,Parcel d,Parcel r,int flags)throws RemoteException{
   if(code==INTERFACE_TRANSACTION){r.writeString(Support.MUSIC);return true;}
   String method=Contracts.name("music",code);
   try{if(active&&method!=null){d.enforceInterface(Support.MUSIC);int source=d.readInt();if(source==1){
    if(method.equals("getCurrentPlayState")){log("[D3] 状态查询="+playing+"，uid="+Binder.getCallingUid());r.writeNoException();r.writeInt(playing?0:1);return true;}if(method.equals("getCurrentPosition")){log("[D4] 进度查询="+position+"，uid="+Binder.getCallingUid());r.writeNoException();r.writeLong(position);return true;}
    if(method.equals("getCurrentMediaData")){r.writeNoException();r.writeInt(1);tracks.get(index).writeToParcel(r,0);log("[C1] USB 当前歌曲查询命中");return true;}
    if(method.equals("getPlayList")){r.writeNoException();r.writeTypedList(tracks);log("[C2] USB 完整列表查询命中：5首");return true;}
    if(method.equals("getUsbPlayList")){try(ParcelFileDescriptor fd=Track.json(ProbeService.this,tracks)){r.writeNoException();r.writeInt(1);fd.writeToParcel(r,0);}log("[C3] USB JSON/PFD 查询命中：5首");return true;}
    if(method.equals("getPlayListAsync")){IBinder cb=d.readStrongBinder();r.writeNoException();work.post(()->{try{try(ParcelFileDescriptor fd=Track.json(ProbeService.this,tracks)){callback(cb,Support.LIST,Contracts.code("list","onUsbPlayListChanged"),p->{p.writeInt(1);p.writeInt(1);fd.writeToParcel(p,0);});}log("[C4] USB异步PFD列表回调已返回：5首");publish();}catch(Exception e){fail("列表回调",e);}});return true;}
    if(method.equals("addMediaStateListener")||method.equals("addMusicStateListener")){IBinder cb=d.readStrongBinder();boolean multi=method.equals("addMediaStateListener");if(cb!=null){boolean known=false;for(Listener l:listeners)if(l.b.equals(cb)&&l.multi==multi)known=true;if(!known)listeners.add(new Listener(cb,multi));}r.writeNoException();log("[C5] USB媒体监听器注册，数量="+listeners.size());work.postDelayed(()->publish(),250);return true;}
    if(method.equals("next")||method.equals("prev")){r.writeNoException();work.post(()->select(index+(method.equals("next")?1:-1)));return true;}
    if(method.equals("playItem")){Track t=d.readInt()!=0?Track.read(d):null;r.writeNoException();if(t!=null){for(int i=0;i<tracks.size();i++)if(tracks.get(i).id.equals(t.id)||tracks.get(i).uri.equals(t.uri)){final int n=i;work.post(()->select(n));return true;}}log("测试点选未匹配，不向原服务发送虚拟文件");return true;}
    if(method.equals("play")||method.equals("pause")||method.equals("seekTo")||method.equals("replay")){long seek=method.equals("seekTo")?d.readLong():0;r.writeNoException();work.post(()->{try{if(method.equals("pause"))pauseAudio();else if(method.equals("seekTo")){if(player!=null)player.seekTo((int)Math.min(Integer.MAX_VALUE,Math.max(0,seek)));}else if(method.equals("replay"))select(index);else resumeAudio();}catch(Exception e){fail("播放控制",e);}});return true;}
    if(method.equals("sendCommand")){int app=d.readInt();String command=d.readString();if(command!=null&&command.startsWith("PLAY_")){r.writeNoException();log("模拟列表命令 "+command+"；本版本仅支持仪表 playItem 点选，不交给原USB播放虚拟路径");return true;}}
   }}
   }catch(Exception e){fail("代理事务 "+method,e);if(r!=null){r.setDataSize(0);r.setDataPosition(0);r.writeException(new IllegalStateException("USB probe: "+e));}return true;}
   d.setDataPosition(0);return music!=null&&music.transact(code,d,r,flags);
  }
 }
 public void onTaskRemoved(Intent i){work.post(()->restore());super.onTaskRemoved(i);}
 public void onDestroy(){boolean wasActive=active;active=false;if(work!=null)work.post(()->{if(wasActive)restore();thread.quitSafely();});if(apiBound)try{unbindService(apiConnection);}catch(Exception ignored){}if(recoveryBound)try{unbindService(recoveryConnection);}catch(Exception ignored){}super.onDestroy();}
}
