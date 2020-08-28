package com.bilibili.brouter.example.lib1.test;

import com.bilibili.brouter.api.BelongsTo;
import com.bilibili.brouter.api.Routes;
import com.bilibili.brouter.example.extensions.base.BaseActivity;

@Routes(value = "coffee://lib1/test2", exported = true)
@BelongsTo("test")
public class Test2Activity extends BaseActivity {
}
