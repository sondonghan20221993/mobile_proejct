package com.example.class_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.class_project.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            replaceFragment(AnalysisFragment(), 0)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_analysis -> {
                    replaceFragment(AnalysisFragment(), 0)
                    true
                }
                R.id.nav_statistics -> {
                    replaceFragment(StatisticsFragment(), 1)
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, tabIndex: Int) {
        val (enterAnim, exitAnim) = if (tabIndex > currentTabIndex) {
            R.anim.slide_in_right to android.R.anim.fade_out
        } else {
            R.anim.slide_in_left to android.R.anim.fade_out
        }
        currentTabIndex = tabIndex
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(enterAnim, exitAnim)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
