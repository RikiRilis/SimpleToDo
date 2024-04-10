package com.rilisentertainment.simpletodo.ui.todo

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rilisentertainment.simpletodo.data.usecase.VibrationUtil
import com.rilisentertainment.simpletodo.R
import com.rilisentertainment.simpletodo.databinding.FragmentTodoBinding
import com.rilisentertainment.simpletodo.domain.TodoInfo
import com.rilisentertainment.simpletodo.domain.TodoList
import com.rilisentertainment.simpletodo.domain.TodosFilter.All
import com.rilisentertainment.simpletodo.domain.TodosFilter.Completed
import com.rilisentertainment.simpletodo.domain.TodosFilter.Pending
import com.rilisentertainment.simpletodo.ui.home.MainActivity
import com.rilisentertainment.simpletodo.ui.todo.adapter.TodoAdapter
import com.rilisentertainment.simpletodo.ui.todo.adapter.TodoListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("DEPRECATION")
@AndroidEntryPoint
class TodoFragment : Fragment() {
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var todoListAdapter: TodoListAdapter

    private val binding get() = _binding!!
    private val todoViewModel by viewModels<TodoViewModel>()
    private val todoListViewModel by viewModels<TodoListViewModel>()
    private val categories = listOf(All, Pending, Completed)

    private var _binding: FragmentTodoBinding? = null
    private var filteredList: MutableList<TodoInfo> = mutableListOf()
    private var pendingCount: MutableList<TodoInfo> = mutableListOf()
    private var arrayOfLists: MutableList<TodoList> = mutableListOf()
    private var currentFilter = categories[0]
    private var currentList = "Default"
    private var interstitialAdMob: InterstitialAd? = null

    companion object {
        const val CURRENT_FILTER = "current_filter"
        const val CURRENT_LIST = "current_list"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
    }

    private fun initUI() {
        chargeList()
        initUIState()
        initList()
        initListeners()
        initAds()
    }

    private fun initAds() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            requireContext(),
            MainActivity.INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAdMob = interstitialAd
                }
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    interstitialAdMob = null
                }
            }
        )
    }

    private fun showAds() {
        interstitialAdMob?.show(requireActivity())
    }

    private fun chargeList() {
        CoroutineScope(Dispatchers.IO).launch {
            val todosListStored: List<TodoInfo> =
                MainActivity.DataManager(requireContext()).getTodosListFromDataStore()
            if (todosListStored.isNotEmpty()) todoViewModel.updateAllList(todosListStored)

            if (
                MainActivity.DataManager(requireContext()).getCurrentListsFromDataStore().isEmpty()
            ) arrayOfLists.add(TodoList("Default"))
            else arrayOfLists = MainActivity.DataManager(
                requireContext()
            ).getCurrentListsFromDataStore()
            todoListViewModel.updateAllList(arrayOfLists)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initUIState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                todoViewModel.todos.collect {
                    todoAdapter.updateList(it)

                    pendingCount.clear()
                    todoViewModel.getList().forEach { item ->
                        if (!item.done && item.list == currentList) {
                            pendingCount.add(item)
                        }
                    }
                    binding.tvTodosCount.text =
                        "${pendingCount.size} ${requireContext().getString(R.string.todos_count)}"

                    withContext(Dispatchers.Main) {
                        selectFilter()
                    }
                }
            }
        }
    }

    private fun initList() {
        todoAdapter = TodoAdapter(
            onItemRemove = { info ->
                deleteTodo(info)
            },

            onItemChecked = { info ->
                checkTodo(info)
            },

            onLongItemSelected = { info ->
                editTodo(info)
            }
        )

        binding.rvTodosElements.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = todoAdapter
        }

        filteredList.addAll(todoViewModel.getList())
        pendingCount.addAll(todoViewModel.getList())

        CoroutineScope(Dispatchers.IO).launch {
            val filter = MainActivity.DataManager(requireContext()).getStrings(CURRENT_FILTER)
            currentList = MainActivity.DataManager(requireContext()).getStrings(
                CURRENT_LIST
            ).ifEmpty {
                "Default"
            }

            binding.tvTodoTitleList.text = currentList

            currentFilter = when (filter) {
                "Pending" -> categories[1]
                "Completed" -> categories[2]
                else -> categories[0]
            }

            withContext(Dispatchers.Main) {
                selectFilter()
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun showDialogAddTodo(flag: String = "") {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_todo)
        dialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        dialog.window!!.setElevation(1F)
        dialog.window!!.setDimAmount(0.4F)
        dialog.window!!.decorView.scaleX = 0.5F
        dialog.window!!.decorView.scaleY = 0.5F
        dialog.window!!.decorView
            .animate()
            .setDuration(200)
            .scaleX(1F)
            .scaleY(1F)
            .start()

        val addBtn: Button = dialog.findViewById(R.id.dialogAddTodoBtn)
        val tiDescInput: TextInputEditText = dialog.findViewById(R.id.dialogTodoDesc)
        val ivAddNewList: ImageView = dialog.findViewById(R.id.ivAddNewList)
        val acListAutoComplete: AutoCompleteTextView = dialog.findViewById(R.id.acListAutoComplete)
        acListAutoComplete.setText(currentList)
        if (flag.isNotEmpty()) tiDescInput.setText(flag)

        val list = mutableListOf<String>()
        arrayOfLists.forEach { item ->
            list.add(item.title)
        }

        val listAdapter = ArrayAdapter(requireContext(), R.layout.item_list, list)
        acListAutoComplete.setAdapter(listAdapter)

        var itemSelected: String = acListAutoComplete.text.toString()

        acListAutoComplete.onItemClickListener = AdapterView
            .OnItemClickListener { parent, _, position, _ ->
                itemSelected = parent.getItemAtPosition(position).toString()
            }

        ivAddNewList.setOnClickListener {
            dialog.dismiss()
            dialog.hide()
            showAddNewListDialog(tiDescInput.text.toString().trim())
        }

        addBtn.setOnClickListener {
            if (tiDescInput.text.toString().trim().isNotEmpty()) {
                val dateFormat = SimpleDateFormat("d/M/yyyy - HH:mm")
                val currentDate = Date()

                todoViewModel.addTodo(
                    TodoInfo(
                        desc = tiDescInput.text.toString().trim(),
                        timestamp = requireContext().getString(R.string.reg_created_timestamp) + " " + dateFormat.format(
                            currentDate
                        ),
                        list = itemSelected
                    )
                )
                todoAdapter.updateList(todoViewModel.getList())
                todoViewModel.saveTodosToDataStore(requireContext())

                selectFilter()
                dialog.dismiss()
                dialog.hide()
            } else {
                VibrationUtil.vibrate2(requireContext())
                tiDescInput.error = requireContext().getString(R.string.dialog_add_reg_error)
            }
        }

        dialog.show()
    }

    private fun showAddNewListDialog(flag: String = "") {
        val dialog = BottomSheetDialog(requireContext())
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_bottom_add_list)
        dialog.behavior.peekHeight = 800
        dialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val etInput: TextInputEditText = dialog.findViewById(R.id.etDialogAddList)!!
        val btnAddList: Button = dialog.findViewById(R.id.btnDialogAddListBtn)!!

        etInput.requestFocus()

        btnAddList.setOnClickListener {
            if (etInput.text.toString().trim() != "") {
                if (todoListViewModel.getTodosList().any {
                        it.title.lowercase() == etInput.text.toString().trim().lowercase()
                    }
                ) {
                    showDialogAddTodo()
                    dialog.dismiss()
                    dialog.hide()
                } else {
                    todoListViewModel.addList(
                        TodoList(etInput.text.toString().trim())
                    )
                    todoListViewModel.saveLists(requireContext())
                    arrayOfLists = todoListViewModel.getTodosList()
                    currentList = etInput.text.toString().trim()
                    binding.tvTodoTitleList.text = currentList
                    CoroutineScope(Dispatchers.IO).launch {
                        MainActivity.DataManager(requireContext()).saveStrings(
                            CURRENT_LIST,
                            currentList
                        )
                    }

                    showDialogAddTodo(flag)
                    dialog.dismiss()
                    dialog.hide()

                    val randomNum = (1..100).random()
                    if (randomNum >= 50) {
                        showAds()
                        initAds()
                    }
                }
            } else {
                VibrationUtil.vibrate2(requireContext())
                etInput.error = requireContext().getString(R.string.dialog_add_reg_error)
            }
        }

        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        dialog.window!!.attributes.windowAnimations = R.style.Bottom_Sheet_Dialog_Anim
        dialog.window!!.setGravity(Gravity.BOTTOM)
    }

    @SuppressLint("InflateParams")
    private fun showFilterDialog() {
        val dialog = BottomSheetDialog(requireContext())
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_bottom_todo_filter)
        dialog.behavior.peekHeight = 900
        dialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val colorFilterGray = PorterDuffColorFilter(
            resources.getColor(R.color.gray),
            PorterDuff.Mode.SRC_IN
        )
        val colorFilterAccent = PorterDuffColorFilter(
            resources.getColor(R.color.accent),
            PorterDuff.Mode.SRC_IN
        )

        val closeBtn: LinearLayout = dialog.findViewById(R.id.ivTodoFilterDialogClose)!!
        val allFilter: LinearLayout = dialog.findViewById(R.id.llTodoFilterAll)!!
        val pendingFilter: LinearLayout = dialog.findViewById(R.id.llTodoFilterPending)!!
        val completedFilter: LinearLayout = dialog.findViewById(R.id.llTodoFilterCompleted)!!
        val rvTodoLists: RecyclerView = dialog.findViewById(R.id.rvTodoLists)!!
        val ivTodoFilterAll: ImageView = dialog.findViewById(R.id.ivTodoFilterAll)!!
        val ivTodoFilterPending: ImageView = dialog.findViewById(R.id.ivTodoFilterPending)!!
        val ivTodoFilterCompleted: ImageView = dialog.findViewById(R.id.ivTodoFilterCompleted)!!

        ivTodoFilterAll.colorFilter = colorFilterGray
        ivTodoFilterPending.colorFilter = colorFilterGray
        ivTodoFilterCompleted.colorFilter = colorFilterGray

        when (currentFilter) {
            All -> ivTodoFilterAll.colorFilter = colorFilterAccent
            Pending -> ivTodoFilterPending.colorFilter = colorFilterAccent
            Completed -> ivTodoFilterCompleted.colorFilter = colorFilterAccent
        }

        todoListAdapter = TodoListAdapter(
            onItemSelected = {
                listSelected(it)
            },

            onItemRemove = {
                listRemove(it)
            }
        )

        initUIListState()

        rvTodoLists.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = todoListAdapter
        }

        allFilter.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            it.startAnimation(animation)

            currentFilter = categories[0]
            CoroutineScope(Dispatchers.IO).launch {
                MainActivity.DataManager(requireContext()).saveStrings(
                    CURRENT_FILTER,
                    "All"
                )
            }

            ivTodoFilterAll.colorFilter = colorFilterAccent
            ivTodoFilterPending.colorFilter = colorFilterGray
            ivTodoFilterCompleted.colorFilter = colorFilterGray

            selectFilter()
        }

        pendingFilter.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            it.startAnimation(animation)

            currentFilter = categories[1]
            CoroutineScope(Dispatchers.IO).launch {
                MainActivity.DataManager(requireContext()).saveStrings(
                    CURRENT_FILTER,
                    "Pending"
                )
            }

            ivTodoFilterAll.colorFilter = colorFilterGray
            ivTodoFilterPending.colorFilter = colorFilterAccent
            ivTodoFilterCompleted.colorFilter = colorFilterGray

            selectFilter()
        }

        completedFilter.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            it.startAnimation(animation)

            currentFilter = categories[2]
            CoroutineScope(Dispatchers.IO).launch {
                MainActivity.DataManager(requireContext()).saveStrings(
                    CURRENT_FILTER,
                    "Completed"
                )
            }

            ivTodoFilterAll.colorFilter = colorFilterGray
            ivTodoFilterPending.colorFilter = colorFilterGray
            ivTodoFilterCompleted.colorFilter = colorFilterAccent

            selectFilter()
        }

        closeBtn.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            it.startAnimation(animation)

            dialog.dismiss()
            dialog.hide()
        }

        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        dialog.window!!.attributes.windowAnimations = R.style.Bottom_Sheet_Dialog_Anim
        dialog.window!!.setGravity(Gravity.BOTTOM)
    }

    private fun initUIListState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                todoListViewModel.list.collect {
                    todoListAdapter.updateList(it)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun listRemove(item: TodoList) {
        todoListViewModel.getTodosList().removeAt(
            getListByName(todoListViewModel.getTodosList(), item.title)
        )
        todoListAdapter.updateList(todoListViewModel.getTodosList())
        todoListAdapter.notifyItemRemoved(
            getListByName(
                todoListViewModel.getTodosList(),
                item.title
            )
        )

        if (todoListViewModel.getTodosList().isEmpty()) {
            todoListViewModel.addList()
            todoListAdapter.notifyItemInserted(0)
        }

        currentList = todoListViewModel.getTodosList()[0].title
        binding.tvTodoTitleList.text = currentList

        todoViewModel.getList().forEach { element ->
            if (element.list == item.title) {
                todoViewModel.todosList = todoViewModel.getList().minusElement(element)
            }
        }
        todoAdapter.updateList(todoViewModel.getList())

        pendingCount.clear()
        todoViewModel.getList().forEach { todo ->
            if (!todo.done && todo.list == currentList) {
                pendingCount.add(todo)
            }
        }
        binding.tvTodosCount.text =
            "${pendingCount.size} ${requireContext().getString(R.string.todos_count)}"

        todoListViewModel.saveLists(requireContext())
        todoViewModel.saveTodosToDataStore(requireContext())
        selectFilter()
    }

    @SuppressLint("SetTextI18n")
    private fun listSelected(item: String) {
        currentList = item
        binding.tvTodoTitleList.text = currentList

        pendingCount.clear()
        todoViewModel.getList().forEach { todo ->
            if (!todo.done && todo.list == currentList) {
                pendingCount.add(todo)
            }
        }
        binding.tvTodosCount.text =
            "${pendingCount.size} ${requireContext().getString(R.string.todos_count)}"

        CoroutineScope(Dispatchers.IO).launch {
            MainActivity.DataManager(requireContext()).saveStrings(
                CURRENT_LIST,
                currentList
            )
        }

        selectFilter()
    }

    private fun getListByName(info: List<TodoList>, title: String): Int {
        return info.indexOfFirst { it.title == title }
    }

    @SuppressLint("SetTextI18n")
    private fun deleteTodo(info: TodoInfo) {
        todoViewModel.todosList = todoViewModel.getList().minusElement(
            todoViewModel.getList()[getTodoById(todoViewModel.getList(), info.id)]
        )
        todoAdapter.updateList(todoViewModel.getList())

        pendingCount.clear()
        todoViewModel.getList().forEach { item ->
            if (!item.done && item.list == currentList) {
                pendingCount.add(item)
            }
        }
        binding.tvTodosCount.text =
            "${pendingCount.size} ${requireContext().getString(R.string.todos_count)}"

        todoViewModel.saveTodosToDataStore(requireContext())
        selectFilter()
    }

    @SuppressLint("SetTextI18n")
    private fun deleteCompleted() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_todo_delete_all_confirm)
        dialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        dialog.window!!.setElevation(1F)
        dialog.window!!.setDimAmount(0.4F)
        dialog.window!!.decorView.scaleX = 0.5F
        dialog.window!!.decorView.scaleY = 0.5F
        dialog.window!!.decorView
            .animate()
            .setDuration(200)
            .scaleX(1F)
            .scaleY(1F)
            .start()

        val yesBtn: TextView = dialog.findViewById(R.id.tvDialogAdvertisementYes)
        val noBtn: TextView = dialog.findViewById(R.id.tvDialogAdvertisementNo)

        yesBtn.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            it.startAnimation(animation)
            VibrationUtil.vibrate1(requireContext())

            todoViewModel.getList().forEach { item ->
                if (item.done && item.list == currentList) {
                    todoViewModel.todosList = todoViewModel.getList().minusElement(item)
                }
            }
            todoAdapter.updateList(todoViewModel.getList())

            pendingCount.clear()
            todoViewModel.getList().forEach { item ->
                if (!item.done && item.list == currentList) {
                    pendingCount.add(item)
                }
            }
            binding.tvTodosCount.text =
                "${pendingCount.size} ${requireContext().getString(R.string.todos_count)}"

            todoViewModel.saveTodosToDataStore(requireContext())
            selectFilter()

            dialog.dismiss()
            dialog.hide()

            val randomNum = (1..100).random()
            if (randomNum >= 50) {
                showAds()
                initAds()
            }
        }

        noBtn.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            it.startAnimation(animation)

            dialog.hide()
        }

        if(todoViewModel.getList().any { it.done }) dialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun checkTodo(info: TodoInfo) {
        todoViewModel.checkTask(getTodoById(todoViewModel.getList(), info.id))
        todoAdapter.updateList(todoViewModel.getList())

        pendingCount.clear()
        todoViewModel.getList().forEach { item ->
            if (!item.done && item.list == currentList) {
                pendingCount.add(item)
            }
        }
        binding.tvTodosCount.text =
            "${pendingCount.size} ${requireContext().getString(R.string.todos_count)}"

        todoViewModel.saveTodosToDataStore(requireContext())
        selectFilter()
    }

    private fun selectFilter() {
        binding.tvTodoEmptyPedingList.visibility = View.GONE
        binding.tvTodoEmptyCompletedList.visibility = View.GONE
        binding.tvTodoEmptyList.visibility = View.GONE

        when (currentFilter) {
            All -> {
                filteredList.clear()
                todoViewModel.getList().forEach { item ->
                    if (item.list == currentList) {
                        filteredList.add(item)
                    }
                }
                todoAdapter.updateList(filteredList.toList())

                if (filteredList.isEmpty()) {
                    binding.tvTodoEmptyList.visibility = View.VISIBLE
                    binding.rvTodosElements.visibility = View.GONE
                } else {
                    binding.tvTodoEmptyList.visibility = View.GONE
                    binding.rvTodosElements.visibility = View.VISIBLE
                }
            }

            Pending -> {
                filteredList.clear()
                todoViewModel.getList().forEach { item ->
                    if (!item.done && item.list == currentList) {
                        filteredList.add(item)
                    }
                }
                todoAdapter.updateList(filteredList.toList())

                if (filteredList.isEmpty()) {
                    binding.tvTodoEmptyPedingList.visibility = View.VISIBLE
                    binding.rvTodosElements.visibility = View.GONE
                } else {
                    binding.tvTodoEmptyPedingList.visibility = View.GONE
                    binding.rvTodosElements.visibility = View.VISIBLE
                }
            }

            else -> {
                filteredList.clear()
                todoViewModel.getList().forEach { item ->
                    if (item.done && item.list == currentList) {
                        filteredList.add(item)
                    }
                }
                todoAdapter.updateList(filteredList.toList())

                if (filteredList.isEmpty()) {
                    binding.tvTodoEmptyCompletedList.visibility = View.VISIBLE
                    binding.rvTodosElements.visibility = View.GONE
                } else {
                    binding.tvTodoEmptyCompletedList.visibility = View.GONE
                    binding.rvTodosElements.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun editTodo(info: TodoInfo) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_todo)
        dialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        dialog.window!!.setElevation(1F)
        dialog.window!!.setDimAmount(0.4F)
        dialog.window!!.decorView.scaleX = 0.5F
        dialog.window!!.decorView.scaleY = 0.5F
        dialog.window!!.decorView
            .animate()
            .setDuration(200)
            .scaleX(1F)
            .scaleY(1F)
            .start()

        val topTitle: TextView = dialog.findViewById(R.id.dialogTodoTopTitle)
        val addBtn: Button = dialog.findViewById(R.id.dialogAddTodoBtn)
        val tiDescInput: TextInputEditText = dialog.findViewById(R.id.dialogTodoDesc)
        val ivAddNewList: ImageView = dialog.findViewById(R.id.ivAddNewList)
        val acListAutoCompleteParent: TextInputLayout =
            dialog.findViewById(R.id.acListAutoCompleteParent)

        ivAddNewList.visibility = View.GONE
        acListAutoCompleteParent.visibility = View.GONE
        topTitle.text = requireContext().getString(R.string.edit_todo_desc)
        addBtn.text = requireContext().getString(R.string.edit)
        tiDescInput.setText(info.desc)

        addBtn.setOnClickListener {
            if (tiDescInput.text.toString().trim().isNotEmpty()) {
                val index = getTodoById(todoViewModel.getList(), info.id)

                todoViewModel.editList(tiDescInput.text.toString().trim(), index)
                todoAdapter.updateList(todoViewModel.getList())

                CoroutineScope(Dispatchers.IO).launch {
                    MainActivity.DataManager(requireContext()).saveTodosList(
                        todoViewModel.getList()
                    )
                }

                selectFilter()
                dialog.dismiss()
                dialog.hide()
            } else {
                VibrationUtil.vibrate2(requireContext())
                tiDescInput.error = requireContext().getString(R.string.dialog_add_reg_error)
            }
        }

        dialog.show()
    }

    private fun getTodoById(info: List<TodoInfo>, id: String): Int {
        return info.indexOfFirst { it.id == id }
    }

    private fun initListeners() {
        binding.addToDoBtn.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            it.startAnimation(animation)

            showDialogAddTodo()
        }

        binding.ivTodoFilter.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            it.startAnimation(animation)

            showFilterDialog()
        }

        binding.todoDeleteCompleted.setOnClickListener {
            val animation: Animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            it.startAnimation(animation)

            deleteCompleted()
        }

        interstitialAdMob?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
            }
            override fun onAdShowedFullScreenContent() {
                interstitialAdMob = null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodoBinding.inflate(layoutInflater, container, false)

        return binding.root
    }
}