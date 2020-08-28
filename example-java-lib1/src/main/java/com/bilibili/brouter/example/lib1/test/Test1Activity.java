package com.bilibili.brouter.example.lib1.test;


import com.bilibili.brouter.api.BelongsTo;
import com.bilibili.brouter.api.Routes;
import com.bilibili.brouter.example.extensions.base.BaseActivity;

@Routes(value = "coffee://lib1/test1", exported = true)
@BelongsTo("test")
public class Test1Activity extends BaseActivity {

}
