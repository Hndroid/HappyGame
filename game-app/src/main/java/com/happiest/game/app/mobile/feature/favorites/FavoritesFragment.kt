package com.happiest.game.app.mobile.feature.favorites

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.paging.cachedIn
import com.happiest.game.R
import com.happiest.game.app.mobile.shared.DynamicGridLayoutManager
import com.happiest.game.app.mobile.shared.GamesAdapter
import com.happiest.game.app.mobile.shared.GridSpaceDecoration
import com.happiest.game.app.mobile.shared.RecyclerViewFragment
import com.happiest.game.app.shared.GameInteractor
import com.happiest.game.lib.library.db.RetrogradeDatabase
import com.happiest.game.common.view.setVisibleOrGone
import javax.inject.Inject

class FavoritesFragment : RecyclerViewFragment() {

    @Inject lateinit var retrogradeDb: RetrogradeDatabase
    @Inject lateinit var gameInteractor: GameInteractor

    private lateinit var favoritesViewModel: FavoritesViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favoritesViewModel = ViewModelProvider(this, FavoritesViewModel.Factory(retrogradeDb))
            .get(FavoritesViewModel::class.java)

        val gamesAdapter = GamesAdapter(R.layout.layout_game_grid, gameInteractor)
        favoritesViewModel.favorites.cachedIn(lifecycle).observe(viewLifecycleOwner) {
            gamesAdapter.submitData(lifecycle, it)
        }

        gamesAdapter.addLoadStateListener {
            emptyView?.setVisibleOrGone(gamesAdapter.itemCount == 0)
        }

        recyclerView?.apply {
            this.adapter = gamesAdapter
            this.layoutManager = DynamicGridLayoutManager(context)

            val spacingInPixels = resources.getDimensionPixelSize(R.dimen.grid_spacing)
            GridSpaceDecoration.setSingleGridSpaceDecoration(this, spacingInPixels)
        }
    }

    @dagger.Module
    class Module
}
