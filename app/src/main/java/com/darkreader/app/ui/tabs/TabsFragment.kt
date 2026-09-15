package com.darkreader.app.ui.tabs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.darkreader.app.data.db.AppDatabase
import com.darkreader.app.data.entity.TabEntity
import com.darkreader.app.databinding.FragmentTabsBinding
import com.darkreader.app.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class TabsFragment : Fragment() {

    private var _binding: FragmentTabsBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabAdapter: TabAdapter
    private var tabsList = mutableListOf<TabEntity>()
    private val tabDao by lazy { AppDatabase.getInstance(requireContext()).tabDao() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadTabs()

        binding.btnCloseAll.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                tabDao.deleteAll()
                withContext(Dispatchers.Main) {
                    tabsList.clear()
                    tabAdapter.notifyDataSetChanged()
                    updateEmptyState()
                }
            }
        }

        binding.btnCloseOthers.setOnClickListener {
            val activeTab = tabsList.find { it.isActive }
            if (activeTab != null) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    tabsList.filter { it.id != activeTab.id }.forEach { tabDao.delete(it) }
                    withContext(Dispatchers.Main) {
                        tabsList.retainAll { it.id == activeTab.id }
                        tabAdapter.notifyDataSetChanged()
                        updateEmptyState()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadTabs()
    }

    private fun setupRecyclerView() {
        tabAdapter = TabAdapter(
            tabs = tabsList,
            onTabClicked = { tab -> openTab(tab) },
            onTabClosed = { tab -> closeTab(tab) }
        )
        binding.recyclerTabs.layoutManager = GridLayoutManager(requireContext(), 1)
        binding.recyclerTabs.adapter = tabAdapter
        binding.recyclerTabs.doOnLayout {
            (binding.recyclerTabs.layoutManager as? GridLayoutManager)?.spanCount = tabSpanCount()
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                Collections.swap(tabsList, fromPos, toPos)
                tabAdapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                updatePositionsInDb()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerTabs)
    }

    private fun loadTabs() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val tabs = tabDao.getAllSync()
            withContext(Dispatchers.Main) {
                tabsList.clear()
                tabsList.addAll(tabs)
                tabAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    private fun openTab(tab: TabEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            tabDao.setActive(tab.id)
            withContext(Dispatchers.Main) {
                (activity as? MainActivity)?.openDocument(
                    Uri.parse(tab.documentUri),
                    tab.fileName,
                    tab.fileType,
                    tab.lastPage
                )
            }
        }
    }

    private fun closeTab(tab: TabEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            tabDao.delete(tab)
            withContext(Dispatchers.Main) {
                val index = tabsList.indexOf(tab)
                if (index != -1) {
                    tabsList.removeAt(index)
                    tabAdapter.notifyItemRemoved(index)
                    updateEmptyState()
                }
            }
        }
    }

    private fun updatePositionsInDb() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            tabsList.forEachIndexed { index, tabEntity ->
                val updated = tabEntity.copy(position = index)
                tabDao.update(updated)
            }
        }
    }

    private fun updateEmptyState() {
        binding.emptyText.visibility = if (tabsList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun tabSpanCount(): Int {
        val minItemWidth = resources.getDimensionPixelSize(com.darkreader.app.R.dimen.tab_min_item_width)
        val width = binding.recyclerTabs.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        return (width / minItemWidth).coerceAtLeast(1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
