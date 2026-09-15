package com.darkreader.app.ui.favourites

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.FavouriteEntity
import com.darkreader.app.databinding.FragmentFavouritesBinding
import com.darkreader.app.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavouritesFragment : Fragment() {

    private var _binding: FragmentFavouritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var favouriteAdapter: FavouriteAdapter
    private var favouritesList = mutableListOf<FavouriteEntity>()
    private val favouriteDao by lazy { AppDatabase.getInstance(requireContext()).favouriteDao() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavouritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadFavourites()
    }

    override fun onResume() {
        super.onResume()
        loadFavourites()
    }

    private fun setupRecyclerView() {
        favouriteAdapter = FavouriteAdapter(
            favourites = favouritesList,
            onFavouriteClicked = { fav -> openFavourite(fav) },
            onFavouriteRemoved = { fav -> removeFavourite(fav) }
        )
        binding.recyclerFavourites.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFavourites.adapter = favouriteAdapter
    }

    private fun loadFavourites() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val favourites = favouriteDao.getAllSync()
            withContext(Dispatchers.Main) {
                favouritesList.clear()
                favouritesList.addAll(favourites)
                favouriteAdapter.notifyDataSetChanged()
                binding.emptyText.visibility = if (favourites.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun openFavourite(fav: FavouriteEntity) {
        (activity as? MainActivity)?.openDocument(
            Uri.parse(fav.filePath),
            fav.fileName,
            fav.fileType
        )
    }

    private fun removeFavourite(fav: FavouriteEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            favouriteDao.delete(fav)
            withContext(Dispatchers.Main) {
                val index = favouritesList.indexOf(fav)
                if (index != -1) {
                    favouritesList.removeAt(index)
                    favouriteAdapter.notifyItemRemoved(index)
                    binding.emptyText.visibility = if (favouritesList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
