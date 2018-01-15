package org.wordpress.android.ui.plugins;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitePluginModel;
import org.wordpress.android.fluxc.model.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PluginBrowserActivity extends AppCompatActivity {
    private SiteModel mSite;
    private List<SitePluginModel> mSitePlugins;

    private RecyclerView mInstalledPluginsRecycler;
    private PluginDirectoryAdapter mInstalledPluginsAdapter;

    private RecyclerView mNewPluginsRecycler;
    private PluginDirectoryAdapter mNewPluginsAdapter;

    private int mRowWidth;
    private int mRowHeight;
    private int mIconSize;

    @Inject PluginStore mPluginStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.plugin_browser_activity);
        mDispatcher.register(this);

        mInstalledPluginsRecycler = findViewById(R.id.installed_plugins_recycler);
        mNewPluginsRecycler = findViewById(R.id.new_plugins_recycler);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
        }

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        findViewById(R.id.text_manage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPlugins(PluginBrowserActivity.this, mSite);
            }
        });

        boolean isLandscape = DisplayUtils.isLandscape(this);
        int margin = getResources().getDimensionPixelSize(R.dimen.margin_extra_large);
        int displayWidth = DisplayUtils.getDisplayPixelWidth(this);
        int displayHeight = DisplayUtils.getDisplayPixelHeight(this);
        int maxRows = isLandscape ? 6 : 4;
        mRowWidth = Math.round(displayWidth / (maxRows - 0.4f));
        mIconSize = mRowWidth - (margin * 2);
        mRowHeight = Math.round(displayHeight / 3.2f); // TODO: landscape

        mInstalledPluginsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mInstalledPluginsRecycler.setHasFixedSize(true);
        mInstalledPluginsAdapter = new PluginDirectoryAdapter(this);
        mInstalledPluginsRecycler.setAdapter(mInstalledPluginsAdapter);

        mNewPluginsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mNewPluginsRecycler.setHasFixedSize(true);
        mNewPluginsAdapter = new PluginDirectoryAdapter(this);
        mNewPluginsRecycler.setAdapter(mNewPluginsAdapter);

        loadInstalledPlugins();
        loadNewPlugins();
    }

    @Override
    protected void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    private void loadInstalledPlugins() {
        mSitePlugins = mPluginStore.getSitePlugins(mSite);
        mInstalledPluginsAdapter.setSitePlugins(mSitePlugins);
    }

    private void loadNewPlugins() {
        // TODO: this is a dummy list generated from site plugins
        List<WPOrgPluginModel> wpOrgPlugins = new ArrayList<>();
        for (SitePluginModel sitePlugin: mSitePlugins) {
            WPOrgPluginModel wpOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
            if (wpOrgPlugin != null) {
                wpOrgPlugins.add(wpOrgPlugin);
            }
        }
        mNewPluginsAdapter.setWPOrgPlugins(wpOrgPlugins);
    }

    private SitePluginModel getSitePluginFromSlug(@Nullable String slug) {
        if (slug != null && mSitePlugins != null) {
            for (SitePluginModel plugin : mSitePlugins) {
                if (slug.equals(plugin.getSlug())) {
                    return plugin;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWPOrgPluginFetched(PluginStore.OnWPOrgPluginFetched event) {
        if (isFinishing()) {
            return;
        }
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "An error occurred while fetching the wporg plugin with type: " + event.error.type);
            return;
        }
        if (!TextUtils.isEmpty(event.pluginSlug)) {
            mInstalledPluginsAdapter.refreshPluginWithSlug(event.pluginSlug);
            mNewPluginsAdapter.refreshPluginWithSlug(event.pluginSlug);
        }
    }

    private class PluginDirectoryAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<Object> mItems = new ArrayList<>();
        private final LayoutInflater mLayoutInflater;

        PluginDirectoryAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            setHasStableIds(true);
        }

        public void setSitePlugins(List<SitePluginModel> items) {
            mItems.clear();
            mItems.addAll(items);

            notifyDataSetChanged();
        }

        public void setWPOrgPlugins(List<WPOrgPluginModel> wpOrgPlugins) {
            // strip HTML here so we don't have to do it in every call to onBindViewHolder
            for (WPOrgPluginModel wpOrgPlugin: wpOrgPlugins) {
                wpOrgPlugin.setAuthorAsHtml(HtmlUtils.fastStripHtml(wpOrgPlugin.getAuthorAsHtml()));
            }

            mItems.clear();
            mItems.addAll(wpOrgPlugins);

            notifyDataSetChanged();
        }

        private Object getItem(int position) {
            if (position < mItems.size()) {
                return mItems.get(position);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public long getItemId(int position) {
            Object item = getItem(position);
            if (item instanceof WPOrgPluginModel) {
                return ((WPOrgPluginModel) item).getId();
            } else if (item instanceof SitePluginModel) {
                return ((SitePluginModel) item).getId();
            }
            return -1;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.plugin_browser_row, parent, false);
            return new PluginViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            PluginViewHolder holder = (PluginViewHolder) viewHolder;
            Object item = getItem(position);
            if (item == null) return;

            SitePluginModel sitePlugin;
            WPOrgPluginModel wpOrgPlugin;
            String name;
            String author;
            if (item instanceof SitePluginModel) {
                sitePlugin = (SitePluginModel) item;
                wpOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
                if (wpOrgPlugin == null) {
                    mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(sitePlugin.getSlug()));
                }
                name = sitePlugin.getName();
                author = sitePlugin.getAuthorName();
            } else {
                wpOrgPlugin = (WPOrgPluginModel) item;
                sitePlugin = null; // TODO: change to getSitePluginFromSlug(wpOrgPlugin.getSlug());
                name = wpOrgPlugin.getName();
                author = wpOrgPlugin.getAuthorAsHtml();
            }

            boolean isUpdatable = sitePlugin != null && PluginUtils.isUpdateAvailable(sitePlugin, wpOrgPlugin);
            String iconUrl = wpOrgPlugin != null ? wpOrgPlugin.getIcon() : null;

            holder.nameText.setText(name);
            holder.authorText.setText(author);
            holder.icon.setImageUrl(iconUrl, ImageType.PLUGIN_ICON);

            if (sitePlugin != null) {
                @StringRes int textResId;
                @ColorRes int colorResId;
                @DrawableRes int drawableResId;
                if (PluginUtils.isUpdateAvailable(sitePlugin, wpOrgPlugin)) {
                    textResId = R.string.plugin_needs_update;
                    colorResId = R.color.alert_yellow;
                    drawableResId = R.drawable.plugin_update_available_icon;
                } else if (sitePlugin.isActive()) {
                    textResId = R.string.plugin_active;
                    colorResId = R.color.alert_green;
                    drawableResId = R.drawable.ic_checkmark_green_24dp;
                } else {
                    textResId = R.string.plugin_inactive;
                    colorResId = R.color.grey;
                    drawableResId = R.drawable.ic_cross_grey_600_24dp;
                }
                holder.statusText.setText(textResId);
                holder.statusText.setTextColor(getResources().getColor(colorResId));
                holder.statusIcon.setImageResource(drawableResId);
                holder.statusContainer.setVisibility(View.VISIBLE);
                holder.ratingBar.setVisibility(View.GONE);
            } else {
                holder.statusContainer.setVisibility(View.GONE);
                holder.ratingBar.setVisibility(View.VISIBLE);
                int rating = StringUtils.stringToInt(wpOrgPlugin.getRating(), 1);
                int averageRating = Math.round(rating / 20f);
                holder.ratingBar.setRating(averageRating);
            }
        }

        private void refreshPluginWithSlug(@NonNull String slug) {
            int index = indexOfPluginWithSlug(slug);
            if (index != -1) {
                notifyItemChanged(index);
            }
        }

        private int indexOfPluginWithSlug(@NonNull String slug) {
            for (int i = 0; i < mItems.size(); i++) {
                Object item = mItems.get(i);
                String itemSlug;
                if ((item instanceof  SitePluginModel)) {
                    itemSlug = ((SitePluginModel) item).getSlug();
                } else {
                    itemSlug = ((WPOrgPluginModel) item).getSlug();
                }
                if (slug.equalsIgnoreCase(itemSlug)) {
                    return i;
                }
            }
            return -1;
        }

        private class PluginViewHolder extends ViewHolder {
            final TextView nameText;
            final TextView authorText;
            final ViewGroup statusContainer;
            final TextView statusText;
            final ImageView statusIcon;
            final WPNetworkImageView icon;
            final RatingBar ratingBar;

            PluginViewHolder(View view) {
                super(view);
                nameText = view.findViewById(R.id.plugin_name);
                authorText = view.findViewById(R.id.plugin_author);
                icon = view.findViewById(R.id.plugin_icon);
                ratingBar = view.findViewById(R.id.rating_bar);

                statusContainer = view.findViewById(R.id.plugin_status_container);
                statusText = statusContainer.findViewById(R.id.plugin_status_text);
                statusIcon = statusContainer.findViewById(R.id.plugin_status_icon);

                view.getLayoutParams().width = mRowWidth;
                view.getLayoutParams().height = mRowHeight;

                icon.getLayoutParams().width = mIconSize;
                icon.getLayoutParams().height = mIconSize;

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        Object item = getItem(position);
                        if (item instanceof SitePluginModel) {
                            SitePluginModel sitePlugin = (SitePluginModel) item;
                            ActivityLauncher.viewPluginDetail(PluginBrowserActivity.this, mSite, sitePlugin);
                        } else {
                            // TODO: show detail for WPOrgPlugin - wait for detail redesign to be merged
                        }
                    }
                });
            }
        }
    }
}