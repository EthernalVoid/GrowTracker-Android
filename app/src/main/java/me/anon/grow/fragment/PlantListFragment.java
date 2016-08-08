package me.anon.grow.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.snackbar.SnackBar;
import com.kenny.snackbar.SnackBarListener;

import java.util.ArrayList;
import java.util.Arrays;

import lombok.Setter;
import me.anon.controller.adapter.PlantAdapter;
import me.anon.controller.adapter.SimpleItemTouchHelperCallback;
import me.anon.grow.AddPlantActivity;
import me.anon.grow.MainActivity;
import me.anon.grow.R;
import me.anon.lib.Views;
import me.anon.lib.event.GardenChangeEvent;
import me.anon.lib.helper.BusHelper;
import me.anon.lib.manager.GardenManager;
import me.anon.lib.manager.PlantManager;
import me.anon.model.Garden;
import me.anon.model.Plant;

/**
 * // TODO: Add class description
 *
 * @author 7LPdWcaW
 * @documentation // TODO Reference flow doc
 * @project GrowTracker
 */
@Views.Injectable
public class PlantListFragment extends Fragment
{
	private PlantAdapter adapter;
	@Setter private Garden garden;

	public static PlantListFragment newInstance(@Nullable Garden garden)
	{
		PlantListFragment fragment = new PlantListFragment();
		fragment.setGarden(garden);

		return fragment;
	}

	@Views.InjectView(R.id.recycler_view) private RecyclerView recycler;

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.plant_list_view, container, false);
		Views.inject(this, view);

		return view;
	}

	@Override public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		getActivity().setTitle(garden == null ? "All" : garden.getName() + " plants");

		adapter = new PlantAdapter(getActivity());
		recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
		recycler.setAdapter(adapter);

		ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
		ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
		touchHelper.attachToRecyclerView(recycler);
	}

	@Override public void onStart()
	{
		super.onStart();

		adapter.setPlants(PlantManager.getInstance().getSortedPlantList(garden));
		adapter.notifyDataSetChanged();
	}

	@Override public void onStop()
	{
		super.onStop();

		ArrayList<Plant> plants = new ArrayList<Plant>();
		ArrayList<String> plantIds = new ArrayList<>();
		plants.addAll(new ArrayList(Arrays.asList(new Plant[adapter.getItemCount()])));
		plantIds.addAll(new ArrayList(Arrays.asList(new String[adapter.getItemCount()])));

		for (Plant plant : PlantManager.getInstance().getPlants())
		{
			int adapterIndex = adapter.getPlants().indexOf(plant);

			if (adapterIndex > -1)
			{
				plants.set(adapterIndex, plant);
				plantIds.set(adapterIndex, plant.getId());
			}
			else
			{
				plants.add(plant);
			}
		}

		if (garden == null)
		{
			PlantManager.getInstance().setPlants(plants);
			PlantManager.getInstance().save();
		}
		else
		{
			garden.setPlantIds(plantIds);
			GardenManager.getInstance().save();
		}
	}

	@Views.OnClick public void onFabAddClick(View view)
	{
		Intent addPlant = new Intent(getActivity(), AddPlantActivity.class);
		startActivity(addPlant);
	}

	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		if (garden != null)
		{
			inflater.inflate(R.menu.plant_list_menu, menu);
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.edit_garden)
		{
			GardenDialogFragment dialogFragment = new GardenDialogFragment(garden);
			dialogFragment.setOnEditGardenListener(new GardenDialogFragment.OnEditGardenListener()
			{
				@Override public void onGardenEdited(Garden garden)
				{
					int index = GardenManager.getInstance().getGardens().indexOf(PlantListFragment.this.garden);
					GardenManager.getInstance().getGardens().set(index, garden);
					GardenManager.getInstance().save();
					PlantListFragment.this.garden = garden;

					getActivity().setTitle(garden == null ? "All" : garden.getName() + " plants");
					adapter.setPlants(PlantManager.getInstance().getSortedPlantList(garden));
					adapter.notifyDataSetChanged();

					((MainActivity)getActivity()).setNavigationView();
				}
			});
			dialogFragment.show(getFragmentManager(), null);

			return true;
		}
		else if (item.getItemId() == R.id.delete_garden)
		{
			new AlertDialog.Builder(getActivity())
				.setTitle("Are you sure?")
				.setMessage(Html.fromHtml("Are you sure you want to delete garden <b>" + garden.getName() + "</b>?"))
				.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					@Override public void onClick(DialogInterface dialogInterface, int i)
					{
						final Garden oldGarden = garden;
						final int oldIndex = GardenManager.getInstance().getGardens().indexOf(garden);

						GardenManager.getInstance().getGardens().remove(garden);
						GardenManager.getInstance().save();

						SnackBar.show(getActivity(), "Garden deleted", "undo", new SnackBarListener()
						{
							@Override public void onSnackBarStarted(Object o){}
							@Override public void onSnackBarFinished(Object o){}

							@Override public void onSnackBarAction(Object o)
							{
								GardenManager.getInstance().getGardens().add(oldIndex, oldGarden);
								GardenManager.getInstance().save();

								BusHelper.getInstance().post(new GardenChangeEvent());
							}
						});

						((MainActivity)getActivity()).setNavigationView();
						((MainActivity)getActivity()).getNavigation().getMenu().findItem(R.id.all).setChecked(true);
						((MainActivity)getActivity()).onNavigationItemSelected(((MainActivity)getActivity()).getNavigation().getMenu().findItem(R.id.all));
					}
				})
				.setNegativeButton("No", null)
				.show();
		}

		return super.onOptionsItemSelected(item);
	}
}
