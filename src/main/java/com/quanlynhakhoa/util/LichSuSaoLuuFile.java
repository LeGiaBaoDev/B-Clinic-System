package com.quanlynhakhoa.util;

import com.quanlynhakhoa.entity.LichSuSaoLuu;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public final class LichSuSaoLuuFile {
    private final Path file;
    public LichSuSaoLuuFile(Path file){this.file=file;}
    public synchronized void ghi(LichSuSaoLuu row)throws IOException{
        Files.createDirectories(file.toAbsolutePath().getParent());
        String line=String.join(",",List.of(row.getThoiGian().toString(),row.getLoai(),row.getDuongDan(),Long.toString(row.getKichThuoc()),row.getKetQua(),row.getNguoiThucHien()).stream().map(LichSuSaoLuuFile::quote).toList())+System.lineSeparator();
        Files.writeString(file,line,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private static String quote(String s){return "\""+s.replace("\r"," ").replace("\n"," ").replace("\"","\"\"")+"\"";}
    public synchronized List<LichSuSaoLuu> doc()throws IOException{
        if(!Files.exists(file))return List.of();var rows=new ArrayList<LichSuSaoLuu>();
        for(String line:Files.readAllLines(file,StandardCharsets.UTF_8)){
            var fields=new ArrayList<String>();var part=new StringBuilder();boolean quoted=false;
            for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){part.append('"');i++;}else quoted=!quoted;}else if(ch==','&&!quoted){fields.add(part.toString());part.setLength(0);}else part.append(ch);}
            fields.add(part.toString());if(quoted||fields.size()!=6)throw new IOException("Lịch sử sao lưu bị lỗi định dạng.");
            try{rows.add(new LichSuSaoLuu(LocalDateTime.parse(fields.get(0)),fields.get(1),fields.get(2),Long.parseLong(fields.get(3)),fields.get(4),fields.get(5)));}catch(RuntimeException e){throw new IOException("Lịch sử sao lưu không hợp lệ",e);}
        }Collections.reverse(rows);return rows;
    }
}
