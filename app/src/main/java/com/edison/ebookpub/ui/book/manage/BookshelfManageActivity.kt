package com.edison.ebookpub.ui.book.manage

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.edison.ebookpub.R
import com.edison.ebookpub.base.VMBaseActivity
import com.edison.ebookpub.constant.AppConst
import com.edison.ebookpub.constant.PreferKey
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.Book
import com.edison.ebookpub.data.entities.BookGroup
import com.edison.ebookpub.data.entities.BookSource
import com.edison.ebookpub.databinding.ActivityArrangeBookBinding
import com.edison.ebookpub.lib.dialogs.alert
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.ui.book.group.GroupManageDialog
import com.edison.ebookpub.ui.book.group.GroupSelectDialog
import com.edison.ebookpub.ui.theme.AppTheme
import com.edison.ebookpub.ui.widget.SelectActionBar
import com.edison.ebookpub.ui.widget.recycler.DragSelectTouchHelper
import com.edison.ebookpub.ui.widget.recycler.ItemTouchCallback
import com.edison.ebookpub.ui.widget.recycler.VerticalDivider
import com.edison.ebookpub.utils.cnCompare
import com.edison.ebookpub.utils.getPrefInt
import com.edison.ebookpub.utils.setEdgeEffectColor
import com.edison.ebookpub.utils.showDialogFragment
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BookshelfManageActivity :
    VMBaseActivity<ActivityArrangeBookBinding, BookshelfManageViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    SelectActionBar.CallBack,
    BookAdapter.CallBack,
    SourcePickerDialog.Callback,
    GroupSelectDialog.CallBack {

    override val binding by viewBinding(ActivityArrangeBookBinding::inflate)
    override val viewModel by viewModels<BookshelfManageViewModel>()
    override val groupList: ArrayList<BookGroup> = arrayListOf()
    private val groupRequestCode = 22
    private val addToGroupRequestCode = 34
    private val adapter by lazy { BookAdapter(this, this) }
    private var booksFlowJob: Job? = null
    private var menu: Menu? = null
    private var groupId: Long = -1

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        groupId = intent.getLongExtra("groupId", -1)
        launch {
            binding.titleBar.subtitle = withContext(IO) {
                appDb.bookGroupDao.getByID(groupId)?.groupName
                    ?: getString(R.string.no_group)
            }
        }
        initView()
        initGroupData()
        initBookData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bookshelf_manage, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        upMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun selectAll(selectAll: Boolean) {
        adapter.selectAll(selectAll)
    }

    override fun revertSelection() {
        adapter.revertSelection()
    }

    override fun onClickSelectBarMainAction() {
        selectGroup(groupRequestCode, 0)
    }

    private fun initView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = getPrefInt(PreferKey.bookshelfSort) == 3
        val dragSelectTouchHelper: DragSelectTouchHelper =
            DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        // When this page is opened, it is in selection mode
        dragSelectTouchHelper.activeSlideSelect()
        // Note: need judge selection first, so add ItemTouchHelper after it.
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
        binding.selectActionBar.setMainActionText(R.string.move_to_group)
        binding.selectActionBar.inflateMenu(R.menu.bookshelf_menage_sel)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
        binding.composeView.setContent {
            AppTheme {
                BatchChangeSourceDialog(
                    state = viewModel.batchChangeSourceState,
                    size = viewModel.batchChangeSourceSize,
                    position = viewModel.batchChangeSourcePosition
                ) {
                    viewModel.batchChangeSourceCoroutine?.cancel()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initGroupData() {
        launch {
            appDb.bookGroupDao.flowAll().conflate().collect {
                groupList.clear()
                groupList.addAll(it)
                adapter.notifyDataSetChanged()
                upMenu()
            }
        }
    }

    private fun initBookData() {
        booksFlowJob?.cancel()
        booksFlowJob = launch {
            when (groupId) {
                AppConst.rootGroupId -> appDb.bookDao.flowNoGroup()
                AppConst.bookGroupAllId -> appDb.bookDao.flowAll()
                AppConst.bookGroupLocalId -> appDb.bookDao.flowLocal()
                AppConst.bookGroupAudioId -> appDb.bookDao.flowAudio()
                AppConst.bookGroupNoneId -> appDb.bookDao.flowNoGroup()
                else -> appDb.bookDao.flowByGroup(groupId)
            }.conflate().map { books ->
                when (getPrefInt(PreferKey.bookshelfSort)) {
                    1 -> books.sortedByDescending {
                        it.latestChapterTime
                    }
                    2 -> books.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }
                    3 -> books.sortedBy {
                        it.order
                    }
                    else -> books.sortedByDescending {
                        it.durChapterTime
                    }
                }
            }.conflate().collect { books ->
                adapter.setItems(books)
            }
        }
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_group_manage -> showDialogFragment<GroupManageDialog>()
            else -> if (item.groupId == R.id.menu_group) {
                binding.titleBar.subtitle = item.title
                groupId = appDb.bookGroupDao.getByName(item.title.toString())?.groupId ?: 0
                initBookData()
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_del_selection ->
                alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
                    okButton { viewModel.deleteBook(*adapter.selection.toTypedArray()) }
                    noButton()
                }
            R.id.menu_update_enable ->
                viewModel.upCanUpdate(adapter.selection, true)
            R.id.menu_update_disable ->
                viewModel.upCanUpdate(adapter.selection, false)
            R.id.menu_add_to_group -> selectGroup(addToGroupRequestCode, 0)
            R.id.menu_change_source -> showDialogFragment<SourcePickerDialog>()
        }
        return false
    }

    private fun upMenu() {
        menu?.findItem(R.id.menu_book_group)?.subMenu?.let { subMenu ->
            subMenu.removeGroup(R.id.menu_group)
            groupList.forEach { bookGroup ->
                subMenu.add(R.id.menu_group, bookGroup.order, Menu.NONE, bookGroup.groupName)
            }
        }
    }

    override fun selectGroup(requestCode: Int, groupId: Long) {
        showDialogFragment(
            GroupSelectDialog(groupId, requestCode)
        )
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
        when (requestCode) {
            groupRequestCode -> adapter.selection.let { books ->
                val array = Array(books.size) {
                    books[it].copy(group = groupId)
                }
                viewModel.updateBook(*array)
            }
            adapter.groupRequestCode -> {
                adapter.actionItem?.let {
                    viewModel.updateBook(it.copy(group = groupId))
                }
            }
            addToGroupRequestCode -> adapter.selection.let { books ->
                val array = Array(books.size) { index ->
                    val book = books[index]
                    book.copy(group = book.group or groupId)
                }
                viewModel.updateBook(*array)
            }
        }
    }

    override fun upSelectCount() {
        binding.selectActionBar.upCountView(adapter.selection.size, adapter.getItems().size)
    }

    override fun updateBook(vararg book: Book) {
        viewModel.updateBook(*book)
    }

    override fun deleteBook(book: Book) {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            okButton {
                viewModel.deleteBook(book)
            }
        }
    }

    override fun sourceOnClick(source: BookSource) {
        viewModel.changeSource(adapter.selection, source)
        viewModel.batchChangeSourceState.value = true
    }

}