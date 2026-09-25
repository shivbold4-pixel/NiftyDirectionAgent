package com.niftydirection.agent;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE=4101, POST_NOTIFICATIONS=4102;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private LinearLayout root; private ImageView preview; private TextView status,result,newsPreview;
    private Uri selectedImage; private Button analyzeButton;
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    @Override protected void onCreate(Bundle b){super.onCreate(b);buildUi();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},POST_NOTIFICATIONS);}
    private void buildUi(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(20));root.setBackgroundColor(Color.rgb(11,18,32));
        TextView title=new TextView(this);title.setText("NIFTY DIRECTION AGENT");title.setTextColor(Color.WHITE);title.setTextSize(23);title.setTypeface(Typeface.DEFAULT_BOLD);root.addView(title,lp(-1,-2,0,0,0,0));
        TextView sub=new TextView(this);sub.setText("Option-chain + price confirmation + live market headlines");sub.setTextColor(Color.rgb(156,163,175));sub.setTextSize(13);root.addView(sub,lp(-1,-2,0,dp(3),0,dp(10)));
        status=card("READY — upload an NSE option-chain screenshot");root.addView(status,lp(-1,-2,0,0,0,dp(12)));
        preview=new ImageView(this);preview.setBackgroundColor(Color.rgb(17,24,39));preview.setScaleType(ImageView.ScaleType.CENTER_CROP);root.addView(preview,lp(-1,dp(180),0,0,0,dp(10)));
        Button pick=button("1. SELECT CHART SCREENSHOT");pick.setOnClickListener(v->selectImage());analyzeButton=button("2. ANALYZE SNAPSHOT + NEWS");analyzeButton.setEnabled(false);analyzeButton.setOnClickListener(v->runAnalysis());Button settings=button("SETTINGS / API KEY");settings.setOnClickListener(v->showSettings());
        root.addView(pick,lp(-1,dp(48),0,0,0,dp(8)));root.addView(analyzeButton,lp(-1,dp(48),0,0,0,dp(8)));root.addView(settings,lp(-1,dp(48),0,0,0,dp(8)));
        root.addView(label("NEXT SNAPSHOT REMINDER"),lp(-1,-2,0,dp(6),0,dp(6)));LinearLayout rr=new LinearLayout(this);rr.setOrientation(LinearLayout.HORIZONTAL);Button r15=button("15 MIN"),r20=button("20 MIN");r15.setOnClickListener(v->setReminder(15));r20.setOnClickListener(v->setReminder(20));rr.addView(r15,lp(0,dp(44),1,0,dp(4),0));rr.addView(r20,lp(0,dp(44),1,0,dp(4),0));root.addView(rr,lp(-1,dp(44),0,0,0,dp(12)));
        root.addView(label("LATEST ANALYSIS"),lp(-1,-2,0,0,0,dp(5)));result=card("No analysis yet.\n\nThe agent will compare this upload with earlier snapshots stored in the app.");root.addView(result,lp(-1,-2,0,0,0,dp(10)));
        root.addView(label("NEWS CONTEXT USED"),lp(-1,-2,0,0,0,dp(5)));newsPreview=card("News headlines will be shown after analysis.");root.addView(newsPreview,lp(-1,-2,0,0,0,dp(10)));
        TextView foot=new TextView(this);foot.setText("Informational market-structure analysis only. This app does not place trades or guarantee direction.");foot.setTextColor(Color.rgb(156,163,175));foot.setTextSize(12);root.addView(foot,lp(-1,-2,0,0,0,0));ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);
    }
    private TextView label(String t){TextView x=new TextView(this);x.setText(t);x.setTextColor(Color.rgb(245,158,11));x.setTextSize(12);x.setTypeface(Typeface.DEFAULT_BOLD);return x;}
    private TextView card(String t){TextView x=new TextView(this);x.setText(t);x.setTextColor(Color.rgb(249,250,251));x.setTextSize(14);x.setGravity(Gravity.START);x.setPadding(dp(12),dp(11),dp(12),dp(11));x.setBackgroundColor(Color.rgb(17,24,39));return x;}
    private Button button(String t){Button b=new Button(this);b.setText(t);b.setTextSize(12);b.setAllCaps(false);return b;}
    private LinearLayout.LayoutParams lp(int w,int h,float weight,int l,int t,int r){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h,weight);p.setMargins(l,t,r,0);return p;}
    private void selectImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMAGE);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==PICK_IMAGE&&res==RESULT_OK&&data!=null&&data.getData()!=null){selectedImage=data.getData();try{getContentResolver().takePersistableUriPermission(selectedImage,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}preview.setImageURI(selectedImage);analyzeButton.setEnabled(true);status.setText("SCREENSHOT LOADED — ready for analysis");}}
    private void runAnalysis(){if(selectedImage==null)return;executor.submit(()->{try{String key=SecretStore.getApiKey(this);if(key==null||key.trim().isEmpty())throw new IllegalStateException("API key is not configured. Open Settings / API Key.");String model=getSharedPreferences("settings",MODE_PRIVATE).getString("model","gpt-5.6-luna");main.post(()->status.setText("ANALYZING OPTION CHAIN + REFRESHING NEWS…"));String a=OpenAiAnalyzer.analyze(this,selectedImage,key,model,buildPriorHistory());saveHistory(a);main.post(()->{status.setText("ANALYSIS COMPLETE");result.setText(formatAnalysis(a));newsPreview.setText(extractNewsSummary(a));});}catch(Exception e){main.post(()->{status.setText("ANALYSIS FAILED");result.setText("Error: "+e.getMessage());});}});}
    private String formatAnalysis(String raw){try{String c=raw.trim();if(c.startsWith("```")&&c.endsWith("```"))c=c.replaceFirst("^```(?:json)?\\\\s*","").replaceFirst("\\\\s*```$","");JSONObject o=new JSONObject(c);StringBuilder s=new StringBuilder();s.append("DIRECTION: ").append(o.optString("direction","UNKNOWN")).append("\nCONFIDENCE: ").append(o.optInt("confidence",0)).append(" / 100\nSTATUS: ").append(o.optString("status","UNKNOWN")).append("\n\n");s.append("SPOT: ").append(value(o,"spot")).append("\nPIVOT: ").append(value(o,"pivot")).append("\nSUPPORT: ").append(join(o.optJSONArray("support"))).append("\nRESISTANCE: ").append(join(o.optJSONArray("resistance"))).append("\n\n");section(s,"OPTION-CHAIN EVIDENCE",o.optJSONArray("option_chain_evidence"));section(s,"WHAT CHANGED",o.optJSONArray("snapshot_change"));s.append("NEWS IMPACT: ").append(o.optString("news_impact","UNKNOWN")).append("\n");section(s,"NEWS SUMMARY",o.optJSONArray("news_summary"));section(s,"BULLISH CONFIRMATION",o.optJSONArray("bullish_confirmation"));section(s,"BEARISH CONFIRMATION",o.optJSONArray("bearish_confirmation"));section(s,"INVALIDATION",o.optJSONArray("invalidation"));section(s,"FALSE-SIGNAL RISK",o.optJSONArray("false_signal_risk"));return s.toString();}catch(Exception e){return raw;}}
    private void section(StringBuilder s,String title,JSONArray a){s.append(title).append(":\n");if(a==null||a.length()==0)s.append("-\n");else for(int i=0;i<a.length();i++)s.append("• ").append(a.optString(i)).append("\n");s.append("\n");}
    private String value(JSONObject o,String k){Object v=o.opt(k);return v==null||v==JSONObject.NULL?"-":v.toString();}
    private String join(JSONArray a){if(a==null||a.length()==0)return "-";StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++){if(i>0)s.append(" / ");s.append(a.optString(i));}return s.toString();}
    private String extractNewsSummary(String raw){try{JSONObject o=new JSONObject(raw.replaceFirst("^```(?:json)?\\\\s*","").replaceFirst("\\\\s*```$",""));JSONArray a=o.optJSONArray("news_summary");if(a==null)return "No structured news summary.";StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++)s.append("• ").append(a.optString(i)).append("\n");return s.toString();}catch(Exception e){return "News was supplied to the model during analysis.";}}
    private String buildPriorHistory(){String h=getSharedPreferences("history",MODE_PRIVATE).getString("items","[]");try{JSONArray a=new JSONArray(h);StringBuilder s=new StringBuilder();for(int i=Math.max(0,a.length()-5);i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null)s.append(x.optString("timestamp","")).append(": ").append(x.optString("analysis","")).append("\n");}return s.toString();}catch(Exception e){return "";}}
    private void saveHistory(String a){try{android.content.SharedPreferences p=getSharedPreferences("history",MODE_PRIVATE);JSONArray x=new JSONArray(p.getString("items","[]"));x.put(new JSONObject().put("timestamp",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ENGLISH).format(new Date())).put("analysis",a));JSONArray t=new JSONArray();for(int i=Math.max(0,x.length()-20);i<x.length();i++)t.put(x.get(i));p.edit().putString("items",t.toString()).apply();}catch(Exception ignored){}}
    private void showSettings(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText key=new EditText(this);key.setHint("OpenAI API key");key.setSingleLine(true);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setTransformationMethod(PasswordTransformationMethod.getInstance());EditText model=new EditText(this);model.setHint("Model ID");model.setSingleLine(true);try{String e=SecretStore.getApiKey(this);if(e!=null)key.setText(e);}catch(Exception ignored){}model.setText(getSharedPreferences("settings",MODE_PRIVATE).getString("model","gpt-5.6-luna"));box.addView(key);box.addView(model);new AlertDialog.Builder(this).setTitle("Agent settings").setMessage("The API key is encrypted with Android Keystore and stored on this device.").setView(box).setPositiveButton("SAVE",(d,w)->{try{String k=key.getText().toString().trim();if(k.isEmpty())SecretStore.clearApiKey(this);else SecretStore.putApiKey(this,k);String m=model.getText().toString().trim();getSharedPreferences("settings",MODE_PRIVATE).edit().putString("model",m.isEmpty()?"gpt-5.6-luna":m).apply();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}).setNegativeButton("CANCEL",null).show();}
    private void setReminder(int min){AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);PendingIntent p=PendingIntent.getBroadcast(this,min,new Intent(this,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);a.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+min*60000L,p);status.setText("REMINDER SET — next screenshot in "+min+" minutes");}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
