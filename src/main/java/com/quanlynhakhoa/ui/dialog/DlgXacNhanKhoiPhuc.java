package com.quanlynhakhoa.ui.dialog;

import com.quanlynhakhoa.dao.BackupDAO;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Bước xác thực mật khẩu; DAO kiểm tra lần nữa ngay khi thực hiện restore. */
public final class DlgXacNhanKhoiPhuc extends JDialog {
    private final JPasswordField password=new JPasswordField(24);
    private final JLabel message=new JLabel("Nhập lại mật khẩu tài khoản quản lý hiện tại.");
    private final JButton ok=new JButton("Xác thực");
    private int attempts;
    private char[] result;
    public DlgXacNhanKhoiPhuc(Window owner,BackupDAO dao){
        super(owner,"Xác nhận khôi phục",ModalityType.APPLICATION_MODAL);
        var content=new JPanel(new BorderLayout(12,12));content.setBorder(BorderFactory.createEmptyBorder(20,24,20,24));
        var warning=new JLabel("Dữ liệu sau thời điểm bản sao lưu sẽ được thay thế.");warning.setForeground(new Color(0xB42318));content.add(warning,BorderLayout.NORTH);
        var middle=new JPanel(new BorderLayout(8,8));middle.add(message,BorderLayout.NORTH);middle.add(password);content.add(middle);
        var actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));var cancel=new JButton("Hủy");cancel.addActionListener(e->dispose());actions.add(cancel);actions.add(ok);content.add(actions,BorderLayout.SOUTH);
        ok.addActionListener(e->{char[] input=password.getPassword();password.setText("");ok.setEnabled(false);password.setEnabled(false);
            new SwingWorker<Boolean,Void>(){
                protected Boolean doInBackground()throws Exception{return dao.xacThuc(input.clone());}
                protected void done(){try{if(!isDisplayable())return;if(get()){result=input.clone();dispose();return;}attempts++;message.setText("Mật khẩu không đúng ("+attempts+"/3).");if(attempts>=3)dispose();}
                    catch(Exception ex){message.setText("Không thể xác thực. Kiểm tra kết nối và quyền quản lý.");}
                    finally{Arrays.fill(input,'\0');ok.setEnabled(true);password.setEnabled(true);password.requestFocusInWindow();}}
            }.execute();});
        setContentPane(content);getRootPane().setDefaultButton(ok);pack();setResizable(false);setLocationRelativeTo(owner);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    public char[] hoi(){setVisible(true);return result;}
}
