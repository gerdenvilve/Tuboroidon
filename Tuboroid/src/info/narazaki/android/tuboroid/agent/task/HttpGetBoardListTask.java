package info.narazaki.android.tuboroid.agent.task;

import info.narazaki.android.lib.agent.http.task.TextHttpGetTaskBase;
import info.narazaki.android.tuboroid.data.BoardData;
import info.narazaki.android.tuboroid.data.BoardIdentifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;

public class HttpGetBoardListTask extends TextHttpGetTaskBase {
    private static final String TAG = "HttpGetBoardListTask";
    
    static public interface Callback {
        void onBoardListReceived(final List<BoardData> data_list);
        
        void onConnectionFailed();
    }
    
    private Pattern pattern_category_;
    private Pattern pattern_board_;
    
    private Callback callback_;
    
    @Override
    protected String getTextEncode() {
        return "MS932";
    }
    
    public HttpGetBoardListTask(String request_uri, Callback callback) {
        super(request_uri);
        callback_ = callback;
        pattern_category_ = Pattern.compile("^<BR\\><BR\\><B\\>([^<]+?)</B\\><BR\\>");
        pattern_board_ = Pattern
                .compile("^<A HREF=http://([\\w\\-]+\\.2ch\\.net|[\\w\\-]+\\.bbspink\\.com)/([\\w\\-]+)/\\>([^<]+?)</A\\>");
    }
    
    @Override
    protected void dispatchHttpTextResponse(HttpResponse res, BufferedReader reader) throws InterruptedException,
            IOException {
        List<BoardData> data_list = new LinkedList<BoardData>();
        String current_category = "";
        
        try {
            long order_id = 1;
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                if (line.startsWith("<BR><BR><B>")) {
                    Matcher matcher = pattern_category_.matcher(line);
                    matcher.reset();
                    if (matcher.find()) {
                        current_category = matcher.group(1);
                    }
                }
                else if (current_category.length() > 0 && line.startsWith("<A HREF=http://")) {
                    Matcher matcher = pattern_board_.matcher(line);
                    matcher.reset();
                    if (matcher.find()) {
                        BoardData data = BoardData.factory(order_id, matcher.group(3), current_category,
                                new BoardIdentifier(matcher.group(1), matcher.group(2), 0, 0));
                        data_list.add(data);
                        order_id++;
                    }
                }
            }
        }
        finally {
            reader.close();
        }
        if (Thread.interrupted()) throw new InterruptedException();
        
        onRequestFinished(data_list);
        
    }
    
    private void onRequestFinished(final List<BoardData> data_list) {
        if (callback_ != null) {
            callback_.onBoardListReceived(data_list);
        }
        callback_ = null;
    }
    
    @Override
    protected void onConnectionError(boolean connectionFailed) {
        if (callback_ != null) {
            callback_.onConnectionFailed();
        }
        callback_ = null;
    }
    
    @Override
    protected void onRequestCanceled() {
        onRequestFinished(null);
    }
    
}
