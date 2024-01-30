package com.drednoot.remembergamething

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController


class MainMenu : Fragment(R.layout.main_menu) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gameButton = view.findViewById<Button>(R.id.newGameMainMenuButton)
        val aboutButton = view.findViewById<Button>(R.id.aboutMainMenuButton)
        (activity as AppCompatActivity).supportActionBar?.hide()

        gameButton.setOnClickListener {
            view.findNavController().navigate(R.id.action_mainMenu_to_gameFragment)
        }
        aboutButton.setOnClickListener {
            view.findNavController().navigate(R.id.action_mainMenu_to_aboutScreen)
        }
    }
}