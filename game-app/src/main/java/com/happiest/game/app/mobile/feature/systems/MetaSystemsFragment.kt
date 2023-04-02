package com.happiest.game.app.mobile.feature.systems

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.happiest.game.R
import com.happiest.game.app.mobile.shared.DynamicGridLayoutManager
import com.happiest.game.app.mobile.shared.GridSpaceDecoration
import com.happiest.game.app.mobile.shared.RecyclerViewFragment
import com.happiest.game.lib.library.MetaSystemID
import com.happiest.game.lib.library.db.RetrogradeDatabase
import com.happiest.game.common.view.setVisibleOrGone
import com.happiest.game.lib.util.subscribeBy
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class MetaSystemsFragment : RecyclerViewFragment() {

    @Inject lateinit var retrogradeDb: RetrogradeDatabase

    private var metaSystemsAdapter: MetaSystemsAdapter? = null

    private lateinit var metaSystemsViewModel: MetaSystemsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        metaSystemsViewModel = ViewModelProvider(this, MetaSystemsViewModel.Factory(retrogradeDb))
            .get(MetaSystemsViewModel::class.java)

        metaSystemsAdapter = MetaSystemsAdapter { navigateToGames(it) }
        metaSystemsViewModel.availableMetaSystems
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(AndroidLifecycleScopeProvider.from(viewLifecycleOwner))
            .subscribeBy {
                metaSystemsAdapter?.submitList(it)
                emptyView?.setVisibleOrGone(it.isEmpty())
            }

        recyclerView?.apply {
            this.adapter = metaSystemsAdapter
            this.layoutManager = DynamicGridLayoutManager(context, 2)

            val spacingInPixels = resources.getDimensionPixelSize(R.dimen.grid_spacing)
            GridSpaceDecoration.setSingleGridSpaceDecoration(this, spacingInPixels)
        }
    }

    private fun navigateToGames(system: MetaSystemID) {
        val dbNames = system.systemIDs
            .map { it.dbname }
            .toTypedArray()

//        val action = MetaSystemsFragmentDirections.actionNavigationSystemsToNavigationGames(dbNames)
//        findNavController().navigate(action)
    }

    @dagger.Module
    class Module
}
