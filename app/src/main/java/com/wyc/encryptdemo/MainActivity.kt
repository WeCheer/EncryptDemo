package com.wyc.encryptdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import net.sqlcipher.database.SQLiteDatabase

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SQLiteDatabase.loadLibs(this)
        encrypt.setOnClickListener {
            DatabaseUtils.encryptDatabase(this, "student.db", "123456")
        }
        decrypt.setOnClickListener {
            DatabaseUtils.decryptDatabase(this, "student.db", "123456")
        }
    }
}
