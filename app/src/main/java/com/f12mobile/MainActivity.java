package com.f12mobile;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;
import android.view.View;
import android.view.ViewGroup;
import org.json.JSONTokener;
import java.net.URL;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private EditText address, commandInput;
    private TextView panel, panelTitle;
    private Button actionButton, secondaryButton;
    private final List<String> consoleLines = new ArrayList<>();
    private final List<String> networkLines = new ArrayList<>();
    private String currentPanel = "Elements";
    private int simulatedWidth = -1;
    private long pageStartedAt;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        web=findViewById(R.id.web);
        address=findViewById(R.id.address);
        panel=findViewById(R.id.panel);
        panelTitle=findViewById(R.id.panelTitle);
        commandInput=findViewById(R.id.commandInput);
        actionButton=findViewById(R.id.panelAction);
        secondaryButton=findViewById(R.id.panelSecondary);

        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        web.setBackgroundColor(Color.WHITE);

        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage m) {
                String line=m.messageLevel()+": "+m.message();
                addConsole(line);
                if(m.message().startsWith("F12NET:")) addNetwork(m.message().substring(7));
                return true;
            }
            @Override public void onProgressChanged(WebView view,int progress) {
                if(progress==100) addNetwork("Page progress: 100%");
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon) {
                pageStartedAt=System.currentTimeMillis();
                addNetwork("Navigation started: "+safeUrl(url));
            }
            @Override public void onPageFinished(WebView view,String url) {
                address.setText(url);
                installNetworkHooks();
                addNetwork("Navigation finished in "+(System.currentTimeMillis()-pageStartedAt)+" ms");
                showPanel(currentPanel);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) {
                return false;
            }
        });

        findViewById(R.id.go).setOnClickListener(v->navigate());
        address.setOnEditorActionListener((v,a,e)->{
            if(a==EditorInfo.IME_ACTION_GO||a==EditorInfo.IME_ACTION_DONE){navigate();return true;}
            return false;
        });
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
        actionButton.setOnClickListener(v->runPanelAction());
        secondaryButton.setOnClickListener(v->runSecondaryAction());
        web.loadUrl("https://example.com");
    }

    private void navigate(){
        String value=address.getText().toString().trim();
        if(value.isEmpty())return;
        if(!value.matches("(?i)^https?://.*")) value="https://"+value;
        try{
            URL u=new URL(value);
            if(!"http".equals(u.getProtocol())&&!"https".equals(u.getProtocol()))throw new Exception();
            web.loadUrl(value);
        }catch(Exception e){Toast.makeText(this,"Enter a valid HTTP or HTTPS URL",Toast.LENGTH_SHORT).show();}
    }

    private void installNetworkHooks(){
        String js="(function(){if(window.__f12Hooks)return;window.__f12Hooks=true;"+
        "function clean(s){return String(s||'').replace(/([?&](?:otp|code|token|password|secret|auth|key)[^=]*=)[^&]*/ig,'$1[REDACTED]');}"+
        "function log(s){console.log('F12NET:'+s);}"+
        "var f=window.fetch;if(f)window.fetch=function(){var u=arguments[0]&&arguments[0].url||arguments[0],t=Date.now();"+
        "return f.apply(this,arguments).then(function(r){log('FETCH '+r.status+' '+clean(u)+' '+(Date.now()-t)+'ms');return r;},function(e){log('FETCH FAILED '+clean(u));throw e;});};"+
        "var o=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(m,u){this.__f12m=m;this.__f12u=u;return o.apply(this,arguments);};"+
        "var s=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.send=function(){var x=this,t=Date.now();"+
        "x.addEventListener('loadend',function(){log('XHR '+x.__f12m+' '+x.status+' '+clean(x.__f12u)+' '+(Date.now()-t)+'ms');});return s.apply(this,arguments);};})();";
        web.evaluateJavascript(js,null);
    }

    private String safeUrl(String s){
        if(s==null)return "";
        return s.replaceAll("(?i)([?&](?:otp|code|token|password|secret|auth|key)[^=]*=)[^&]*","$1[REDACTED]");
    }
    private void addConsole(String s){
        if(consoleLines.size()>=300)consoleLines.remove(0);
        consoleLines.add(s);
        if(currentPanel.equals("Console"))render(consoleLines);
    }
    private void addNetwork(String s){
        if(networkLines.size()>=500)networkLines.remove(0);
        networkLines.add(safeUrl(s));
        if(currentPanel.equals("Network"))render(networkLines);
    }

    private void showPanel(String name){
        currentPanel=name;
        panelTitle.setText(name.toUpperCase(Locale.US));
        commandInput.setText("");
        commandInput.setVisibility(View.GONE);
        actionButton.setVisibility(View.GONE);
        secondaryButton.setVisibility(View.GONE);
        switch(name){
            case "Elements":
                configure("CSS selector (e.g. body, #app)", "Inspect", null, null);
                runJs("document.documentElement?document.documentElement.outerHTML:'No document'", "DOM snapshot");
                break;
            case "Styles":
                configure("CSS rules (e.g. body { color: red; })", "Apply CSS", "Remove injected CSS", "Remove CSS");
                panel.setText("Enter CSS rules and apply them to the current page. Changes are temporary and reset on reload.");
                break;
            case "Console":
                render(consoleLines);
                configure(null,"Clear",null,null);
                break;
            case "Network":
                render(networkLines);
                configure(null,"Clear",null,null);
                break;
            case "Sources":
                configure(null,"Refresh source",null,null);
                runJs("document.documentElement?document.documentElement.outerHTML:'No document'", "Page source");
                break;
            case "Storage":
                configure(null,"Read storage","Clear local storage","Clear storage");
                runJs("(function(){var a=[];try{for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i),v=localStorage.getItem(k);a.push(k+' = '+(/token|pass|otp|secret|auth|key/i.test(k)?'[REDACTED]':String(v).slice(0,300)));}}catch(e){a.push('localStorage: '+e)};return 'Local storage\\n'+a.join('\\n')+'\\n\\nCookies (values hidden): '+document.cookie.split(';').map(function(x){return x.split('=')[0]+'=[hidden]'}).join('; ');})()", "Storage");
                break;
            case "Performance":
                configure(null,"Capture metrics",null,null);
                runJs("(function(){var p=performance,t=p.getEntriesByType('navigation')[0],r=p.getEntriesByType('resource');return 'Navigation timing\\n'+(t?JSON.stringify({dns:t.domainLookupEnd-t.domainLookupStart,connect:t.connectEnd-t.connectStart,response:t.responseEnd-t.responseStart,domContentLoaded:t.domContentLoadedEventEnd,load:t.loadEventEnd},null,2):'Navigation timing unavailable')+'\\n\\nResources: '+r.length+'\\nTransfer size: '+r.reduce(function(a,x){return a+(x.transferSize||0)},0)+' bytes';})()", "Performance");
                break;
            case "Responsive":
                configure("Viewport width in dp (e.g. 390)", "Set width", "Reset width", "Reset");
                panel.setText("Responsive preview changes the WebView's viewport width. It is a basic viewport simulation, not a full device emulator.");
                break;
            case "Settings":
                configure(null,"Toggle JavaScript","Clear browser cache","Clear cache");
                panel.setText("JavaScript: enabled\\nDOM storage: enabled\\nZoom controls: pinch / built-in\\nOnly inspect websites you are authorized to test. Secrets are masked on a best-effort basis.");
                break;
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
                String selector=input.replace("\\","\\\\").replace("'","\\'");
                runJs("(function(){var e=document.querySelector('"+selector+"');return e?e.outerHTML:'No element matches selector';})()", "Element");
                break;
            case "Styles":
                if(!input.isEmpty()){
                    String css=input.replace("\\","\\\\").replace("</style","<\\/style");
                    runJs("(function(){var s=document.getElementById('__f12_user_styles');if(!s){s=document.createElement('style');s.id='__f12_user_styles';document.head.appendChild(s);}s.textContent='"+css.replace("'","\\'")+"';return 'CSS applied';})()", "Styles");
                }
                break;
            case "Console": consoleLines.clear();render(consoleLines);break;
            case "Network": networkLines.clear();render(networkLines);break;
            case "Sources": runJs("document.documentElement?document.documentElement.outerHTML:'No document'", "Page source");break;
            case "Storage": runJs("JSON.stringify({localStorageLength:localStorage.length,cookieNames:document.cookie.split(';').map(function(x){return x.split('=')[0]})},null,2)", "Storage summary");break;
            case "Performance": runJs("JSON.stringify({navigation:performance.getEntriesByType('navigation').length,resources:performance.getEntriesByType('resource').length,memory:performance.memory?{used:performance.memory.usedJSHeapSize,total:performance.memory.totalJSHeapSize}: 'not exposed'},null,2)", "Performance metrics");break;
            case "Responsive":
                try{int w=Integer.parseInt(input);if(w<240||w>1200)throw new Exception();simulatedWidth=w;applyViewportWidth();panel.setText("Viewport width set to "+w+" dp.");}
                catch(Exception e){Toast.makeText(this,"Enter a width between 240 and 1200 dp",Toast.LENGTH_SHORT).show();}
                break;
            case "Settings":
                boolean enabled=web.getSettings().getJavaScriptEnabled();
                web.getSettings().setJavaScriptEnabled(!enabled);
                panel.setText("JavaScript is now "+(!enabled?"enabled":"disabled")+". Reload the page for the setting to fully apply.");
                break;
        }
    }

    private void runSecondaryAction(){
        switch(currentPanel){
            case "Styles": runJs("(function(){var s=document.getElementById('__f12_user_styles');if(s)s.remove();return 'Injected CSS removed';})()", "Styles");break;
            case "Storage":
                web.evaluateJavascript("try{localStorage.clear();'Local storage cleared'}catch(e){String(e)}",v->panel.setText(decode(v)));
                break;
            case "Responsive":
                simulatedWidth=-1;applyViewportWidth();panel.setText("Viewport width reset to full screen.");break;
            case "Settings":
                web.clearCache(true);CookieManager.getInstance().flush();panel.setText("WebView cache cleared. Cookies were not cleared.");break;
        }
    }

    private void applyViewportWidth(){
        ViewGroup.LayoutParams lp=web.getLayoutParams();
        lp.width=simulatedWidth<0?ViewGroup.LayoutParams.MATCH_PARENT:(int)(simulatedWidth*getResources().getDisplayMetrics().density);
        web.setLayoutParams(lp);
        web.setHorizontalScrollBarEnabled(simulatedWidth>0);
        if(simulatedWidth>0)web.setBackgroundColor(Color.WHITE);
    }

    private void runJs(String js,String label){
        web.evaluateJavascript(js,v->panel.setText(label+"\\n"+decode(v)));
    }
    private String decode(String value){
        if(value==null)return "";
        try{Object o=new JSONTokener(value).nextValue();return String.valueOf(o);}
        catch(Exception e){return value;}
    }
    private void render(List<String> lines){
        StringBuilder b=new StringBuilder();
        for(String s:lines)b.append(s).append('\n');
        panel.setText(b.length()==0?"No entries yet.":b.toString());
    }
    @Override public void onBackPressed(){
        if(web.canGoBack())web.goBack();else super.onBackPressed();
    }
}