package simonxianyu.buildinfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * 实现 collect goal
 * Created by simon on 16/5/22.
 */
@Mojo(name = "collect")
public class BuildInfoMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}")
  private MavenProject project;

  @Parameter(property = "projectName", defaultValue = "${project.artifactId}")
  private String projectName;

  @Parameter(property = "scmType", defaultValue = "git")
  private String scmType;


  public void execute() throws MojoExecutionException, MojoFailureException {
//    project = MavenSession.getCurrentProject();
    Properties buildProp = new Properties();
    buildProp.put("build.os", System.getProperty("os.name"));
    buildProp.put("project.name", projectName);
    buildProp.put("project.version", project.getVersion());
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    buildProp.put("project.build.time", sdf.format(new Date()));
    String buildNumber = System.getenv("BUILD_NUMBER");
    if (null != buildNumber) {
      buildProp.put("jenkins.buildNumber", buildNumber);
    }
    if ("git".equalsIgnoreCase(scmType)) {
      fillGitInfo(buildProp);
    }

    FileOutputStream fout = null;
    try {
      fout = new FileOutputStream(project.getBuild().getOutputDirectory() +"/build-info.properties");
      buildProp.store(fout, "project build information");
    } catch (IOException e) {
      e.printStackTrace();
      getLog().error("Failed to write build properties");
    } finally {
      if (null != fout) {
        try {
          fout.close();
        } catch (IOException e) {
//          e.printStackTrace();
          getLog().warn("Failed to close outputstream");
        }
      }
    }

  }

  private void fillGitInfo(Properties buildProp) {
    String result;

    if (project.hasParent()) {
      try {
        String headContent = FileUtils.readFileToString(new File(project.getParent().getBasedir(),".git/HEAD"));
        buildProp.put("git.head.file_content", headContent);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    result = execShell("git rev-parse HEAD");
    if (null != result) {
      buildProp.put("git.head.commit", result);
    }

    result = execShell("git rev-list HEAD --count");
    if (result != null) {
      buildProp.put("git.rev-list.count", result);
    }

    result = execShell("git log -1");
    if (result != null) {
      buildProp.put("git.last.log", result);
    }
    result = execShell("git status");
    if (result != null) {
      buildProp.put("git.status", result);
    }
  }

  private String execShell(String... args) {
    String[] cmd = new String[args.length+2];
    String osName = System.getProperty("os.name");
    osName = osName == null ? "":osName.toLowerCase();
    int index=2;
    if (osName.contains("mac")
        || osName.contains("linux")
        || osName.contains("cygwin") ) {
      cmd[0]="/bin/sh";
      cmd[1]="-c";
      index=2;
    } else if(osName.contains("win" )) {
      cmd[0]="cmd.exe";
      cmd[1]="/C";
    } else {
      throw new RuntimeException("unknown os "+osName);
    }
    for(String a : args) {
      cmd[index]=a;
      index++;
    }
    String line;
    BufferedReader br = null;
    Process process;
    try {
      process = Runtime.getRuntime().exec(cmd);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    StringBuilder strb = new StringBuilder(100);
    try {
      if (null != process) {
        br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        process.waitFor();
      } else {
        getLog().error("Failed to create process");
        return null;
      }
      boolean begin = true;
      while((line = br.readLine())!=null) {
        if (begin ) {
          begin = false;
        } else {
          strb.append('\n');
        }
        strb.append(line);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(br);
    }
    return strb.toString();
  }

  public void setProject(MavenProject project) {
    this.project = project;
  }
}
