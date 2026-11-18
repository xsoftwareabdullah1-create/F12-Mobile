package com.f12mobile;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.net.URL;
import java.util.*;

public class MainActivity extends Activity {
    private static final int CREATE_HAR = 41;
    private WebView web;
    private EditText address, commandInput;
    private TextView panel, panelTitle;
    private Button actionButton, secondaryButton;
    private final List<String> consoleLines = new ArrayList<>();
    private final List<JSONObject> networkEntries = new ArrayList<>();
    private String currentPanel = "Elements";
    private int simulatedWidth = -1;
    private long pageStartedAt;
    private String lastRequestDetail = "No request selected yet.";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        WebView.setWebContentsDebuggingEnabled(true);
        setContentView(R.layout.activity_main);
        web=findViewById(R.id.web); address=findViewById(R.id.address);
        panel=findViewById(R.id.panel); panelTitle=findViewById(R.id.panelTitle);
        commandInput=findViewById(R.id.commandInput);
        actionButton=findViewById(R.id.panelAction); secondaryButton=findViewById(R.id.panelSecondary);
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true); settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false); settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        web.setBackgroundColor(Color.WHITE);
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage m) {
                String msg=m.message();
                if(msg.startsWith("F12DETAIL:")) {
                    try { JSONObject o=new JSONObject(msg.substring(10)); addNetworkEntry(o); }
                    catch(Exception ignored) { addConsole("Network detail event could not be parsed."); }
                } else addConsole(m.messageLevel()+": "+msg);
                return true;
            }
            @Override public void onProgressChanged(WebView view,int progress) {
                if(progress==100) addConsole("Page load progress: 100%");
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon) {
                pageStartedAt=System.currentTimeMillis();
                addNetworkEntry(event("navigation","GET",safeUrl(url),0,0,"Navigation started"));
            }
            @Override public void onPageFinished(WebView view,String url) {
                address.setText(url);
                installNetworkHooks();
                addConsole("Navigation completed in "+(System.currentTimeMillis()-pageStartedAt)+" ms");
                showPanel(currentPanel);
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request) {
                String u=request.getUrl().toString(), scheme=request.getUrl().getScheme();
                if(scheme!=null&&(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https"))) {
                    JSONObject o=event("resource",request.getMethod(),safeUrl(u),0,0,request.isForMainFrame()?"document":"resource");
                    try { JSONObject h=new JSONObject(); for(Map.Entry<String,String> e:request.getRequestHeaders().entrySet()) {
                        String k=e.getKey(); h.put(k, sensitiveHeader(k)?"[REDACTED]":e.getValue());
                    } o.put("requestHeaders",h); } catch(Exception ignored) {}
                    runOnUiThread(()->addNetworkEntry(o));
                }
                return null;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) { return false; }
        });
        findViewById(R.id.go).setOnClickListener(v->navigate());
        address.setOnEditorActionListener((v,a,e)->{if(a==EditorInfo.IME_ACTION_GO||a==EditorInfo.IME_ACTION_DONE){navigate();return true;}return false;});
        findViewById(R.id.back).setOnClickListener(v->{if(web.canGoBack())web.goBack();});
        findViewById(R.id.forward).setOnClickListener(v->{if(web.canGoForward())web.goForward();});
        findViewById(R.id.reload).setOnClickListener(v->web.reload());
        findViewById(R.id.elements).setOnClickListener(v->showPanel("Elements"));
        findViewById(R.id.styles).setOnClickListener(v->showPanel("Styles"));
        findViewById(R.id.console).setOnClickListener(v->showPanel("Console"));
        findViewById(R.id.network).setOnClickListener(v->showPanel("Network"));
        findViewById(R.id.source).setOnClickListener(v->showPanel("Sources"));
        findViewById(R.id.storage).setOnClickListener(v->showPanel("Storage"));
        findViewById(R.id.performance).setOnClickListener(v->showPanel("Performance"));
        findViewById(R.id.responsive).setOnClickListener(v->showPanel("Responsive"));
        findViewById(R.id.settings).setOnClickListener(v->showPanel("Settings"));
        findViewById(R.id.accessibility).setOnClickListener(v->showPanel("Accessibility"));
        actionButton.setOnClickListener(v->runPanelAction());
        secondaryButton.setOnClickListener(v->runSecondaryAction());
        web.loadUrl("https://example.com");
    }

    private JSONObject event(String type,String method,String url,long duration,int status,String note) {
        JSONObject o=new JSONObject();
        try{o.put("type",type);o.put("method",method);o.put("url",url);o.put("durationMs",duration);o.put("status",status);o.put("note",note);o.put("time",System.currentTimeMillis());}catch(Exception ignored){}
        return o;
    }
    private boolean sensitiveHeader(String k){return k!=null&&k.matches("(?i)authorization|cookie|set-cookie|x-api-key|proxy-authorization");}
    private String redact(String s){
        if(s==null)return "";
        return s.replaceAll("(?i)([?&](?:otp|code|token|password|secret|auth|key)[^=]*=)[^&]*","$1[REDACTED]")
                .replaceAll("(?i)(\"?(?:password|passwd|otp|token|secret|authorization)\"?\\s*[:=]\\s*\")([^\"]*)(\")","$1[REDACTED]$3");
    }
    private String safeUrl(String s){return redact(s);}
    private void installNetworkHooks(){
        String js="(function(){if(window.__f12Hooks)return;window.__f12Hooks=true;"+
        "function clean(s){return String(s||'').replace(/([?&](?:otp|code|token|password|secret|auth|key)[^=]*=)[^&]*/ig,'$1[REDACTED]');}"+
        "function emit(o){try{console.log('F12DETAIL:'+JSON.stringify(o));}catch(e){}}"+
        "function preview(s){s=String(s||'').slice(0,1200);return s.replace(/(\"?(?:password|passwd|otp|token|secret|authorization)\"?\\s*[:=]\\s*\")([^\"]*)(\")/ig,'$1[REDACTED]$3');}"+
        "var f=window.fetch;if(f)window.fetch=function(input,init){var u=input&&input.url||input,m=(init&&init.method)||'GET',t=Date.now(),body=init&&init.body;"+
        "var hs={};try{var h=new Headers((init&&init.headers)||(input&&input.headers));h.forEach(function(v,k){hs[k]=/authorization|cookie|api-key/i.test(k)?'[REDACTED]':v;});}catch(e){}"+
        "return f.apply(this,arguments).then(function(r){var c=r.clone();c.text().then(function(tx){emit({type:'fetch',method:m,url:clean(u),status:r.status,durationMs:Date.now()-t,requestHeaders:hs,payloadPreview:preview(body),responsePreview:preview(tx)});});return r;},function(e){emit({type:'fetch',method:m,url:clean(u),status:0,durationMs:Date.now()-t,note:'Request failed'});throw e;});};"+
        "var op=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(m,u){this.__f12={method:m,url:u,headers:{}};return op.apply(this,arguments);};"+
        "var sh=XMLHttpRequest.prototype.setRequestHeader;XMLHttpRequest.prototype.setRequestHeader=function(k,v){if(this.__f12)this.__f12.headers[k]=/authorization|cookie|api-key/i.test(k)?'[REDACTED]':v;return sh.apply(this,arguments);};"+
        "var se=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.send=function(b){var x=this,t=Date.now(),d=this.__f12||{};x.addEventListener('loadend',function(){emit({type:'xhr',method:d.method||'GET',url:clean(d.url),status:x.status,durationMs:Date.now()-t,requestHeaders:d.headers||{},payloadPreview:preview(b),responsePreview:preview(x.responseText)});});return se.apply(this,arguments);};})();";
        web.evaluateJavascript(js,null);
    }
    private void addNetworkEntry(JSONObject o){
        try {
            if(o.has("url"))o.put("url",safeUrl(o.optString("url")));
            if(networkEntries.size()>=500)networkEntries.remove(0);
            networkEntries.add(o);
            lastRequestDetail=formatDetail(o);
            if(currentPanel.equals("Network"))renderNetwork();
        }catch(Exception ignored){}
    }
    private String formatDetail(JSONObject o){
        return "REQUEST DETAIL\n"+o.optString("method")+" "+o.optString("url")+"\nStatus: "+o.optInt("status")+
        "   Duration: "+o.optLong("durationMs")+" ms\n\nRequest headers (sensitive headers masked):\n"+
        o.optJSONObject("requestHeaders")+"\n\nPayload preview (best-effort redaction):\n"+o.optString("payloadPreview","Not available")+
        "\n\nResponse preview (first 1,200 characters):\n"+o.optString("responsePreview","Not available");
    }
    private void addConsole(String s){if(consoleLines.size()>=300)consoleLines.remove(0);consoleLines.add(s);if(currentPanel.equals("Console"))render(consoleLines);}
    private void navigate(){
        String value=address.getText().toString().trim();if(value.isEmpty())return;
        if(!value.matches("(?i)^https?://.*"))value="https://"+value;
        try{URL u=new URL(value);if(!"http".equals(u.getProtocol())&&!"https".equals(u.getProtocol()))throw new Exception();web.loadUrl(value);}
        catch(Exception e){Toast.makeText(this,"Enter a valid HTTP or HTTPS URL",Toast.LENGTH_SHORT).show();}
    }
    private void showPanel(String name){
        currentPanel=name;panelTitle.setText(name.toUpperCase(Locale.US));commandInput.setText("");
        commandInput.setVisibility(View.GONE);actionButton.setVisibility(View.GONE);secondaryButton.setVisibility(View.GONE);
        switch(name){
            case "Elements":
                configure("CSS selector to inspect (e.g. body)", "Inspect node", "Edit text / HTML", "Apply edit");
                runJs("Array.from(document.querySelectorAll('body,body *')).slice(0,180).map(function(e){return '  '.repeat(Math.min((function(n){var d=0;while(n&&n.parentElement){d++;n=n.parentElement;}return d-1;})(e),8))+'<'+e.tagName.toLowerCase()+(e.id?' #'+e.id:'')+(e.className&&typeof e.className==='string'?' .'+e.className.trim().replace(/\\s+/g,'.'):'')+'>'}).join('\\n')","DOM tree (select a CSS selector to inspect)");
                break;
            case "Styles": configure("CSS rules (e.g. body { color: navy; })","Apply live CSS","Remove CSS","Remove");panel.setText("Live page stylesheet editor. Rules are temporary and reset when the page reloads.");break;
            case "Console":render(consoleLines);configure("JavaScript expression","Run JS",null,null);break;
            case "Network":renderNetwork();configure(null,"Show latest detail","Export HAR","Export");break;
            case "Sources":configure(null,"Refresh source",null,null);runJs("document.documentElement?document.documentElement.outerHTML:'No document'","Page source");break;
            case "Storage":
                configure(null,"Inspect storage","Clear local storage","Clear");
                runJs("(function(){var a=[];try{for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i),v=localStorage.getItem(k);a.push('localStorage: '+k+' [value hidden; '+String(v).length+' chars]');}}catch(e){a.push(String(e));}try{if(indexedDB.databases)indexedDB.databases().then(function(ds){console.log('IndexedDB databases: '+ds.map(function(d){return d.name}).join(', '));});}catch(e){}return a.join('\\n')+'\\nCookie names: '+document.cookie.split(';').map(function(x){return x.split('=')[0]+'=[value hidden]'}).join('; ')+'\\nCookie values are intentionally not displayed.';})()","Storage inspector");break;
            case "Performance":
                configure(null,"Capture timeline",null,null);
                runJs("(function(){var p=performance;return p.getEntries().slice(-160).map(function(e){return e.entryType+' | '+e.name+' | start '+e.startTime.toFixed(1)+' ms | duration '+e.duration.toFixed(1)+' ms'+(e.transferSize?' | '+e.transferSize+' bytes':'')}).join('\\n')||'No performance entries';})()","Performance timeline (recent entries)");break;
            case "Responsive":configure("Viewport width in dp (240–1200)","Set width","Reset viewport","Reset");panel.setText("Viewport presets change the visible WebView width. Device profile selection can also change the user-agent.");break;
            case "Settings":configure(null,"Toggle JavaScript","Clear cache","Clear");panel.setText("JavaScript: enabled\nWebView remote debugging: enabled (USB / desktop Chrome)\nHTTP cleartext: disabled\nOnly inspect sites you are authorized to test.");break;
            case "Accessibility":configure(null,"Refresh tree",null,null);runJs("Array.from(document.querySelectorAll('a,button,input,select,textarea,[role],[aria-label]')).slice(0,300).map(function(e){return '<'+e.tagName.toLowerCase()+'> role='+(e.getAttribute('role')||'')+' name='+(e.getAttribute('aria-label')||e.innerText||e.getAttribute('title')||'').replace(/\\s+/g,' ').slice(0,100)+' id='+e.id}).join('\\n')||'No accessible controls found'","Accessibility tree (DOM-derived)");break;
        }
    }
    private void configure(String hint,String action,String second,String secondAction){
        if(hint!=null){commandInput.setVisibility(View.VISIBLE);commandInput.setHint(hint);commandInput.setSingleLine(false);}
        if(action!=null){actionButton.setVisibility(View.VISIBLE);actionButton.setText(action);}
        if(second!=null){secondaryButton.setVisibility(View.VISIBLE);secondaryButton.setText(second);}
    }
    private void runPanelAction(){
        String input=commandInput.getText().toString().trim();
        switch(currentPanel){
            case "Elements":
                if(input.isEmpty())input="body";
                runJs("(function(){var e=document.querySelector("+JSONObject.quote(input)+");return e?e.outerHTML:'No matching element';})()","Selected DOM node");
                break;
            case "Styles":
                if(!input.isEmpty())runJs("(function(){var s=document.getElementById('__f12_user_styles');if(!s){s=document.createElement('style');s.id='__f12_user_styles';document.head.appendChild(s);}s.textContent="+JSONObject.quote(input)+";return 'Styles applied live';})()","Styles");
                break;
            case "Console":if(!input.isEmpty())runJs("(function(){try{return String("+input+")}catch(e){return String(e)}})()","Console result");break;
            case "Network":panel.setText(lastRequestDetail);break;
            case "Sources":runJs("document.documentElement?document.documentElement.outerHTML:'No document'","Page source");break;
            case "Storage":runJs("JSON.stringify({localStorageKeys:Array.from({length:localStorage.length},(_,i)=>localStorage.key(i)),cookieNames:document.cookie.split(';').map(x=>x.split('=')[0])},null,2)","Storage summary");break;
            case "Performance":runJs("performance.getEntries().slice(-200).map(e=>({type:e.entryType,name:e.name,start:e.startTime,duration:e.duration,transferSize:e.transferSize||0}))","Performance timeline");break;
            case "Responsive":
                try{int w=Integer.parseInt(input);if(w<240||w>1200)throw new Exception();simulatedWidth=w;applyViewportWidth();panel.setText("Viewport width set to "+w+" dp.");}
                catch(Exception e){Toast.makeText(this,"Enter a width between 240 and 1200 dp",Toast.LENGTH_SHORT).show();}break;
            case "Settings":
                boolean enabled=web.getSettings().getJavaScriptEnabled();web.getSettings().setJavaScriptEnabled(!enabled);
                panel.setText("JavaScript is now "+(!enabled?"enabled":"disabled")+". Reload to apply fully.\n\nFor JS breakpoints and stepping, connect Chrome on desktop to chrome://inspect while USB debugging is enabled.");break;
            case "Accessibility":runJs("Array.from(document.querySelectorAll('a,button,input,select,textarea,[role],[aria-label]')).map(e=>({tag:e.tagName,role:e.getAttribute('role'),label:e.getAttribute('aria-label')||e.innerText||e.title,id:e.id,disabled:!!e.disabled}))","Accessibility tree");break;
        }
    }
    private void runSecondaryAction(){
        switch(currentPanel){
            case "Styles":runJs("(function(){var s=document.getElementById('__f12_user_styles');if(s)s.remove();return 'Injected CSS removed';})()","Styles");break;
            case "Network":exportHar();break;
            case "Storage":web.evaluateJavascript("try{localStorage.clear();'Local storage cleared'}catch(e){String(e)}",v->panel.setText(decode(v)));break;
            case "Responsive":simulatedWidth=-1;applyViewportWidth();panel.setText("Viewport reset to full width.");break;
            case "Settings":web.clearCache(true);panel.setText("WebView cache cleared. Cookies and site storage were not cleared.");break;
            case "Elements":
                String sel=commandInput.getText().toString().trim();if(sel.isEmpty())sel="body";
                runJs("(function(){var e=document.querySelector("+JSONObject.quote(sel)+");if(!e)return 'No matching element';e.innerHTML="+JSONObject.quote("<div>Edited by F12 Mobile</div>")+";return 'Selected element HTML replaced. Use the Styles panel for CSS.';})()","DOM edit");break;
        }
    }
    private void renderNetwork(){
        StringBuilder b=new StringBuilder("Tap Show latest detail for request headers and previews.\n\n");
        for(int i=networkEntries.size()-1;i>=0;i--){JSONObject o=networkEntries.get(i);b.append(o.optString("method")).append("  ").append(o.optInt("status")).append("  ").append(o.optString("url")).append("  ").append(o.optLong("durationMs")).append(" ms\n");}
        panel.setText(b.toString());
    }
    private void exportHar(){
        try{
            JSONObject log=new JSONObject();log.put("version","1.2");log.put("creator",new JSONObject().put("name","F12 Mobile").put("version","1.1.0"));
            JSONArray entries=new JSONArray();
            for(JSONObject o:networkEntries){
                JSONObject e=new JSONObject();e.put("startedDateTime",new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US).format(new Date(o.optLong("time"))));
                e.put("time",o.optLong("durationMs"));e.put("request",new JSONObject().put("method",o.optString("method")).put("url",o.optString("url")).put("httpVersion","")); 
                e.put("response",new JSONObject().put("status",o.optInt("status")).put("statusText","").put("headers",new JSONArray()).put("content",new JSONObject().put("mimeType","").put("text",o.optString("responsePreview",""))));
                e.put("timings",new JSONObject().put("send",0).put("wait",o.optLong("durationMs")).put("receive",0));entries.put(e);
            }
            log.put("entries",entries);
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"f12-mobile-session.har");startActivityForResult(i,CREATE_HAR);
            getPreferences(MODE_PRIVATE).edit().putString("pending_har",log.toString(2)).apply();
        }catch(Exception ex){Toast.makeText(this,"Could not prepare HAR export",Toast.LENGTH_SHORT).show();}
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==CREATE_HAR&&resultCode==RESULT_OK&&data!=null){
            try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){
                String har=getPreferences(MODE_PRIVATE).getString("pending_har","{}");out.write(har.getBytes("UTF-8"));Toast.makeText(this,"HAR session report saved",Toast.LENGTH_LONG).show();
            }catch(Exception e){Toast.makeText(this,"HAR export failed",Toast.LENGTH_SHORT).show();}
        }
    }
    private void applyViewportWidth(){
        ViewGroup.LayoutParams lp=web.getLayoutParams();lp.width=simulatedWidth<0?ViewGroup.LayoutParams.MATCH_PARENT:(int)(simulatedWidth*getResources().getDisplayMetrics().density);
        web.setLayoutParams(lp);web.setHorizontalScrollBarEnabled(simulatedWidth>0);
    }
    private void runJs(String js,String label){web.evaluateJavascript(js,v->panel.setText(label+"\n"+decode(v)));}
    private String decode(String value){if(value==null)return "";try{return String.valueOf(new JSONTokener(value).nextValue());}catch(Exception e){return value;}}
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}