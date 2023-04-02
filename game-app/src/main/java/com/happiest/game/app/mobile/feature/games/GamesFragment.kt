package com.happiest.game.app.mobile.feature.games

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.happiest.game.R
import com.happiest.game.app.mobile.shared.GamesAdapter
import com.happiest.game.app.mobile.shared.RecyclerViewFragment
import com.happiest.game.app.shared.GameInteractor
import com.happiest.game.lib.library.db.RetrogradeDatabase
import javax.inject.Inject

class GamesFragment : RecyclerViewFragment() {

    @Inject lateinit var retrogradeDb: RetrogradeDatabase
    @Inject lateinit var gameInteractor: GameInteractor

//    private val args: GamesFragmentArgs by navArgs()

    private lateinit var gamesViewModel: GamesViewModel

    private var gamesAdapter: GamesAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gamesAdapter = GamesAdapter(R.layout.layout_game_list, gameInteractor)

        gamesViewModel = ViewModelProvider(this, GamesViewModel.Factory(retrogradeDb))
            .get(GamesViewModel::class.java)

        gamesViewModel.games.observe(viewLifecycleOwner) { pagedList ->
            gamesAdapter?.submitData(lifecycle, pagedList)
        }

//        args.systemIds.let {
//            gamesViewModel.systemIds.value = listOf(*it)
//        }

        recyclerView?.apply {
            adapter = gamesAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    @dagger.Module
    class Module
}
