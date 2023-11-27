package com.app.rehabcoach;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextEmail, editTextPassword,editTextConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextUsername = findViewById(R.id.editText);
        editTextEmail = findViewById(R.id.editText4);
        editTextConfirm=findViewById(R.id.editText3);
        editTextPassword = findViewById(R.id.editText2);
    }

    public void registerClick(View view) {
        String username = editTextUsername.getText().toString();
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();
        String confirm=editTextConfirm.getText().toString();
        String message="注册成功！\n用户名：" + username + "\n电子邮件：" + email;;
        // 在这里执行注册逻辑，例如向服务器发送注册请求等
        // 在这个示例中，我们只显示一个 Toast 作为演示
        if(username==null) {
            message = "用户名不能为空！";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        if(password==null){
            message = "密码不能为空！";
            Toast.makeText(this,message, Toast.LENGTH_LONG).show();
        }
        if(confirm==null) {
            message = "确认密码不能为空！";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        if(email==null) {
            message = "邮箱不能为空！";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        if(confirm!=password) {
            message = "输入密码不一致！";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        // 在这里执行注册逻辑，例如向服务器发送注册请求等
        // 在这个示例中，我们只显示一个 Toast 作为演示
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

    }
}
