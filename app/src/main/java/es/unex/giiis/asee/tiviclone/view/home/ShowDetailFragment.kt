package es.unex.giiis.asee.tiviclone.view.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import es.unex.giiis.asee.tiviclone.R
import es.unex.giiis.asee.tiviclone.api.APIError
import es.unex.giiis.asee.tiviclone.api.getNetworkService
import es.unex.giiis.asee.tiviclone.data.Repository
import es.unex.giiis.asee.tiviclone.data.model.Show
import es.unex.giiis.asee.tiviclone.data.model.User
import es.unex.giiis.asee.tiviclone.data.toShow
import es.unex.giiis.asee.tiviclone.database.TiviCloneDatabase
import es.unex.giiis.asee.tiviclone.databinding.FragmentShowDetailBinding
import kotlinx.coroutines.launch

private const val TAG = "ShowDetailFragment"

/**
 * A simple [Fragment] subclass.
 * Use the [ShowDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ShowDetailFragment : Fragment() {

    private lateinit var repository: Repository
    private lateinit var user: User
    private lateinit var db: TiviCloneDatabase

    private var _binding: FragmentShowDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ShowDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        db = TiviCloneDatabase.getInstance(context)!!
        repository = Repository.getInstance(db.userDao(),db.showDao(),getNetworkService())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val show = args.show

        val userProvider = activity as UserProvider
        user = userProvider.getUser()

        lifecycleScope.launch{
            Log.d(TAG, "Fetching ${show.title} details")
            try{
               val _show = repository.fetchShowDetail(show.showId).toShow()
               _show.isFavorite = show.isFavorite
               showBinding(_show)
            } catch (error: APIError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG, "Showing ${show.title} details")
        }

    }

    private fun showBinding(show: Show) {
        binding.swFav.isChecked = show.isFavorite
        binding.tvShowTitle.text = show.title
        binding.tvDescription.text = show.description
        binding.tvYear.text = show.year
        binding.swFav.isChecked = show.isFavorite

        Glide.with(this)
            .load(show.imagePath)
            .placeholder(R.drawable.placeholder)
            .into(binding.coverImg)

        Glide.with(this)
            .load(show.bannerPath)
            .placeholder(R.drawable.placeholder)
            .into(binding.bannerImg)

        binding.swFav.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                if (isChecked) {
                    show.isFavorite = true
                    repository.showToLibrary(show,user.userId!!)
                } else {
                    show.isFavorite = false
                    repository.deleteShowFromLibrary(show,user.userId!!)
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment ShowDetailFragment.
         */
        @JvmStatic
        fun newInstance() =
            ShowDetailFragment()
    }
}