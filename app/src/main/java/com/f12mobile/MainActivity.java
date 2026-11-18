package com.f12mobile;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;
import java.net.URL;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private EditText address;
    private TextView panel, panelTitle;
    private final List<String> consoleLines = new ArrayList<>();
    private final List<String> networkLines = new ArrayList<>();
    private String currentPanel = "Elements";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        web=findViewById(R.id.web); address=findViewById(R.id.address);
        panel=findViewById(R.id.panel); panelTitle=findViewById(R.id.panelTitle);
        web.setBackgroundColor(Color.WHITE);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        // Logging-only bridge: no SMS, account, filesystem, or device-control methods.
        web.addJavascriptInterface(new InspectorBridge(), "F12Inspector");
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage m) {
                addConsole(m.messageLevel()+": "+m.message()); return true;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view,String url) {
                address.setText(url); installInspector(); addNetwork("Loaded "+safeUrl(url)); showPanel(currentPanel);
            }
        });
        findViewById(R.id.go).setOnClickListener(v->navigate());
        address.setOnEditorActionListener((v,a,e)->{if(a==EditorInfo.IME_ACTION_GO||a==EditorInfo.IME_ACTION_DONE){navigate();return true;}return false;});
        findViewById(R.id.elements).setOnClickListener(v->showPanel("Elements"));
        findViewById(R.id.console).setOnClickListener(v->showPanel("Console"));
        findViewById(R.id.network).setOnClickListener(v->showPanel("Network"));
        findViewById(R.id.source).setOnClickListener(v->showPanel("Source"));
        web.loadUrl("https://example.com");
    }
    private void navigate(){
        String value=address.getText().toString().trim(); if(value.isEmpty())return;
        if(!value.matches("(?i)^https?://.*"))value="https://"+value;
        try{URL u=new URL(value);if(!"http".equals(u.getProtocol())&&!"https".equals(u.getProtocol()))throw new Exception();web.loadUrl(value);}
        catch(Exception e){Toast.makeText(this,"Enter a valid HTTP or HTTPS URL",Toast.LENGTH_SHORT).show();}
    }
    private void installInspector(){
        String js="(function(){if(window.__f12Installed)return;window.__f12Installed=true;"+
        "function clean(s){return String(s).replace(/([?&](?:otp|code|token|password|secret|auth)[^=]*=)[^&]*/ig,'$1[REDACTED]');}"+
        "function log(s){try{F12Inspector.log(clean(s));}catch(e){}}"+
        "window.addEventListener('error',e=>log('Error: '+e.message));"+
        "const f=window.fetch;window.fetch=function(){let u=arguments[0]&&arguments[0].url||arguments[0],t=Date.now();"+
        "return f.apply(this,arguments).then(r=>{log('FETCH '+r.status+' '+clean(u)+' '+(Date.now()-t)+'ms');return r;}).catch(e=>{log('FETCH FAILED '+clean(u));throw e;});};"+
        "const o=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(m,u){this.__fm=m;this.__fu=u;return o.apply(this,arguments);};"+
        "const s=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.send=function(){let x=this,t=Date.now();"+
        "x.addEventListener('loadend',()=>log('XHR '+x.__fm+' '+x.status+' '+clean(x.__fu)+' '+(Date.now()-t)+'ms'));return s.apply(this,arguments);};})();";
        web.evaluateJavascript(js,null);
    }
    private class InspectorBridge { @JavascriptInterface public void log(String message){runOnUiThread(()->addNetwork(message));} }
    private String safeUrl(String s){if(s==null)return "";return s.replaceAll("(?i)([?&](?:otp|code|token|password|secret|auth)[^=]*=)[^&]*","$1[REDACTED]");}
    private void addConsole(String s){if(consoleLines.size()>200)consoleLines.remove(0);consoleLines.add(s);if(currentPanel.equals("Console"))render(consoleLines);}
    private void addNetwork(String s){if(networkLines.size()>300)networkLines.remove(0);networkLines.add(s);if(currentPanel.equals("Network"))render(networkLines);}
    private void showPanel(String name){
        currentPanel=name;panelTitle.setText(name.toUpperCase(Locale.US));
        if(name.equals("Elements")||name.equals("Source")){
            web.evaluateJavascript("(function(){return document.documentElement?document.documentElement.outerHTML.slice(0,14000):'No document';})()",v->panel.setText(v==null?"No page":v));
        }else if(name.equals("Console"))render(consoleLines);else render(networkLines);
    }
    private void render(List<String> lines){StringBuilder b=new StringBuilder();for(String s:lines)b.append(s).append('\n');panel.setText(b.length()==0?"No entries yet.":b.toString());}
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
