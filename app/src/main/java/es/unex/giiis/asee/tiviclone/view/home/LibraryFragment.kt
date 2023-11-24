package es.unex.giiis.asee.tiviclone.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import es.unex.giiis.asee.tiviclone.api.getNetworkService
import es.unex.giiis.asee.tiviclone.data.Repository
import es.unex.giiis.asee.tiviclone.databinding.FragmentLibraryBinding
import es.unex.giiis.asee.tiviclone.data.model.Show
import es.unex.giiis.asee.tiviclone.data.model.User
import es.unex.giiis.asee.tiviclone.database.TiviCloneDatabase
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LibraryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LibraryFragment : Fragment() {

    private lateinit var user: User
    private lateinit var db: TiviCloneDatabase
    private lateinit var repository: Repository

    private lateinit var listener: OnShowClickListener
    interface OnShowClickListener {
        fun onShowClick(show: Show)
    }

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: LibraryAdapter

    private var favShows: List<Show> = emptyList()

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        db = TiviCloneDatabase.getInstance(context)!!
        repository = Repository.getInstance(db.userDao(),db.showDao(), getNetworkService())
        if (context is OnShowClickListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnShowClickListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()

        val userProvider = activity as UserProvider
        user = userProvider.getUser()

        loadFavorites()
    }

    private fun setUpRecyclerView() {
        adapter = LibraryAdapter(shows = favShows, onClick = {
            listener.onShowClick(it)
        },
            onLongClick = {
                setNoFavorite(it)
                loadFavorites()
                Toast.makeText(context, "${it.title} removed from library", Toast.LENGTH_SHORT).show()
            },
            context = context
        )
        with(binding) {
            rvLibShowList.layoutManager = GridLayoutManager(context, 3)
            rvLibShowList.adapter = adapter
        }
        android.util.Log.d("DiscoverFragment", "setUpRecyclerView")
    }

    private fun loadFavorites(){
        lifecycleScope.launch {
            binding.spinner.visibility = View.VISIBLE
            favShows = db.showDao().getUserWithShows(user.userId!!).shows
            adapter.updateData(favShows)
            binding.spinner.visibility = View.GONE
        }
    }
    private fun setNoFavorite(show: Show){
        lifecycleScope.launch {
            show.isFavorite = false
            repository.deleteShowFromLibrary(show,user.userId!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // avoid memory leaks
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LibraryFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LibraryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}