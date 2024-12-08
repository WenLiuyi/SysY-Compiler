package frontend;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class WriteFile {
    public String filePath;     //文件路径
    public ArrayList<String> lines;

    public WriteFile(){}
    public void write(String filePath,List<String> lines){
        try(BufferedWriter writer= new BufferedWriter(new FileWriter(filePath,false))){
            int len=lines.size();
            for(int i=0;i<len;i++){
                String line=lines.get(i);
                writer.write(line);
                writer.newLine();
            }
        }catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
