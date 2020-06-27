package com.dm6801.filemanager_example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.dm6801.filemanager.FileInfo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initFileManager()
        initDebugButton()
    }

    private fun initDebugButton() {
        debug_button?.setOnClickListener {
            initFileManager()
            /*try {
                lifecycleScope.launch {
                    FileInfo.getDirectoryInfo(filesDir.absolutePath ?: return@launch).await()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }*/
        }
    }

    private fun initFileManager() {
        file_manager_view?.openDirectory(filesDir?.parent ?: return)
    }

}