package simonxianyu.buildinfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Show build-info.properties in html.
 * Created by simon on 16/5/23.
 */
public class BuildInfoServlet extends HttpServlet {

  private static final long serialVersionUID = 8463426604598387311L;
  private String content;

  @Override
  public void init() throws ServletException {
    Properties properties = new Properties();
    StringBuilder strb = new StringBuilder(4096);
    strb
        .append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">\n")
        .append("<title>Build information </title>\n")
        .append("</head>\n")
        .append("<style>\n")
        .append("tr.head{background-color:#d2d6de;}\n")
        .append("td{border-top: 1px solid #f4f4f4;}\n")
        .append(".odd th{background-color:#ccccff;}\n")
        .append(".odd td{background-color:#cccccc;}\n")
        .append("</style>\n")
        .append("<body>");
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/build-info.properties");
      if (null != in) {
        properties.load(in);
      }
      if (properties.size()>0) {
        // print information table
        List<String> propNames = new ArrayList<>(properties.stringPropertyNames());
        Collections.sort(propNames);
        strb.append("<table>\n<tr class=\"head\"><th>Key</th><th>Value</th></tr>\n");
        int stripFlag = 0;
        for(String key: propNames) {
          strb.append("<tr class=\"").append(stripFlag==1?"odd":"even").append("\"><th>").append(key).append("</th><td><pre>")
              .append(properties.getProperty(key)).append("</pre></td></tr>");
          stripFlag = 1-stripFlag;
        }
        strb.append("</table>\n");
      }
    } catch (IOException e) {
//      e.printStackTrace();
      strb.append("<h3>Error in reading information</h3><h4>")
          .append(e.getMessage()).append("</h4>\n<pre>\n");
      StringWriter writer = new StringWriter();
      e.printStackTrace(new PrintWriter(writer));
      strb.append(writer.getBuffer().toString());
      strb.append("\n</pre>\n");
    } finally {
      if (null != in) {
        try {
          in.close();
        } catch (IOException e) {
          // Just ignore.
        }
      }
    }
    this.content = strb.toString();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    PrintWriter writer = resp.getWriter();
    resp.setStatus(200);
    writer.write(content);
  }
}
