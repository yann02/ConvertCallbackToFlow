package com.yyw.callbackflow

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yyw.callbackflow.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mVm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            lifecycleOwner = this@MainActivity
            vm = mVm
            btnStart.setOnClickListener {
                mVm.startCoroutine()
            }
            btnCancel.setOnClickListener {
                mVm.cancelCoroutine()
            }
            btnIncrease.setOnClickListener {
                mVm.increase()
            }
        }
        setContentView(binding.root)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mVm.res.collect {
//                        Log.d("wyy", "it:$it")
                        binding.tvContent.text = it.toString()
                    }
                }
            }
        }
    }
}