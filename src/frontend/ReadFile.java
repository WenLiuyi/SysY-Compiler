package frontend;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadFile {
    public String filePath;     //文件路径
    public List<String> lines;
    public ReadFile(){}
    public void read(String filePath){
        List<String> lines = new ArrayList<>(); // 创建一个列表来保存行

        try(BufferedReader br= new BufferedReader(new FileReader(filePath))){
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line); // 保存每一行
            }
        }catch (IOException e) {
            System.out.println(e.getMessage());
        }
        this.lines=lines;
    }
}
