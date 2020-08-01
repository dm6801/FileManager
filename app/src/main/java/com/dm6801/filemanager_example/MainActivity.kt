package com.dm6801.filemanager_example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initFileManager()
        initDebugButton()
    }

    private fun initDebugButton() {
        debug_button?.setOnClickListener {
            //initFileManager()
        }
    }

    private fun initFileManager() {
        file_manager_view?.settings(
            sideMenu = true,
            popupMenu = true
        )
        file_manager_view?.openDirectory(filesDir?.parent ?: return)
    }

}