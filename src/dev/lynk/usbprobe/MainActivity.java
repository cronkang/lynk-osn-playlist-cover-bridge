package dev.lynk.usbprobe;
import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import java.io.*;

public final class MainActivity extends Activity {
 private TextView logs;private final Handler h=new Handler();
 private final Runnable refresh=new Runnable(){public void run(){logs.setText(Support.logs(MainActivity.this));h.postDelayed(this,1000);}};
 public void onCreate(Bundle state){super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,12,20,12);TextView title=new TextView(this);title.setText("USB 仪表验证 0.3.2 · Android 9");title.setTextSize(24);root.addView(title);
  TextView note=new TextView(this);note.setText("拔掉 U 盘，暂停其它播放器。\n申请USB音源并实际播放五首公版MIDI；3分钟自动停止并恢复服务。测试中不要强停或卸载。");root.addView(note);
  button(root,"1  只读检查 OneOS 和原车 USB",()->command("check"));
  button(root,"2  无U盘启动五首USB音乐（3分钟）",()->new AlertDialog.Builder(this).setTitle("无U盘播放验证").setMessage("将临时注册USB媒体代理、申请USB音源并播放内置音乐。无U盘启动尚未实测，3分钟恢复媒体服务。请停车测试并调低音量。").setNegativeButton("取消",null).setPositiveButton("启用",(d,w)->command("enable")).show());
  LinearLayout row=new LinearLayout(this);for(int i=0;i<5;i++){final int n=i;Button b=new Button(this);b.setText("歌曲 "+(i+1));b.setOnClickListener(v->{Intent it=new Intent(this,ProbeService.class).setAction("select").putExtra("index",n);startForegroundService(it);});row.addView(b,new LinearLayout.LayoutParams(0,52,1));}root.addView(row);
  button(root,"暂停测试音频",()->command("pauseAudio"));
  button(root,"继续测试音频",()->command("resumeAudio"));
  button(root,"立即恢复原车媒体服务",()->command("restore"));
  button(root,"导出完整日志",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"usb-probe-diagnostics.txt");startActivityForResult(i,9);});
  button(root,"卸载自身（先停止测试并恢复）",()->{Intent i=new Intent(this,ProbeService.class).setAction("uninstall");i.putExtra("result",new ResultReceiver(h){protected void onReceiveResult(int code,Bundle data){if(isFinishing()||isDestroyed())return;if(code==1){try{startActivity(new Intent(Intent.ACTION_DELETE,android.net.Uri.parse("package:"+getPackageName())));}catch(Exception e){Support.log(MainActivity.this,"打开卸载界面失败："+e);new AlertDialog.Builder(MainActivity.this).setMessage("无法打开系统卸载界面，请从系统应用设置卸载。").setPositiveButton("知道了",null).show();}}else new AlertDialog.Builder(MainActivity.this).setTitle("暂未卸载").setMessage("未确认原车媒体服务恢复成功，请先导出日志并重试恢复。").setPositiveButton("知道了",null).show();}});startForegroundService(i);});
  logs=new TextView(this);logs.setTextSize(13);logs.setTextIsSelectable(true);ScrollView logScroll=new ScrollView(this);logScroll.addView(logs);root.addView(logScroll,new LinearLayout.LayoutParams(-1,300));ScrollView outer=new ScrollView(this);outer.addView(root);setContentView(outer);Support.log(this,"UI 打开 USB Probe 0.3.2 / Android "+Build.VERSION.RELEASE);}
 private void button(LinearLayout r,String s,Runnable task){Button b=new Button(this);b.setText(s);b.setOnClickListener(v->task.run());r.addView(b,new LinearLayout.LayoutParams(-1,52));}
 private void command(String s){startForegroundService(new Intent(this,ProbeService.class).setAction(s));}
 protected void onResume(){super.onResume();h.post(refresh);}protected void onPause(){h.removeCallbacks(refresh);super.onPause();}
 protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==9&&result==RESULT_OK&&data!=null)try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(Support.logs(this).getBytes("UTF-8"));}catch(Exception e){Support.log(this,"导出失败："+e);}}
}
