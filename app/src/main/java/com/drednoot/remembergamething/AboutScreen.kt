package com.drednoot.remembergamething

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class AboutScreen : Fragment(R.layout.about_screen) {
    private lateinit var menuProvider: AboutMenuProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tv = view.findViewById<TextView>(R.id.myContacts)
        tv.movementMethod = LinkMovementMethod.getInstance()
        (activity as AppCompatActivity).supportActionBar?.show()
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.about_title)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        menuProvider = AboutMenuProvider(this)
        activity?.addMenuProvider(menuProvider)
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            (activity as AppCompatActivity).supportActionBar?.hide()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        activity?.removeMenuProvider(menuProvider)
        super.onDestroyView()
    }
}

class AboutMenuProvider(private val aboutFragment: AboutScreen) : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        android.R.id.home -> {
            aboutFragment.findNavController().popBackStack()
        }

        else -> false
    }
}
