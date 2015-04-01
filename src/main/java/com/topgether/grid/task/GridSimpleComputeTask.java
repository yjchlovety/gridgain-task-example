package com.topgether.grid.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.gridgain.grid.Grid;
import org.gridgain.grid.GridException;
import org.gridgain.grid.cache.GridCache;
import org.gridgain.grid.cache.GridCacheEntry;
import org.gridgain.grid.compute.GridComputeJob;
import org.gridgain.grid.compute.GridComputeJobAdapter;
import org.gridgain.grid.compute.GridComputeJobResult;
import org.gridgain.grid.compute.GridComputeTaskName;
import org.gridgain.grid.compute.GridComputeTaskSplitAdapter;
import org.gridgain.grid.logger.GridLogger;
import org.gridgain.grid.resources.GridInstanceResource;
import org.gridgain.grid.resources.GridLocalNodeIdResource;
import org.gridgain.grid.resources.GridLoggerResource;
import org.gridgain.grid.util.GridClassLoaderCache;

/**
 * a demo for simple cumpute
 * @author wangwei
 * 2015年4月1日 下午1:29:02
 */
@GridComputeTaskName("GridSimpleComputeTask")
public class GridSimpleComputeTask extends GridComputeTaskSplitAdapter<Object, Long> {

	private static final long serialVersionUID = 1L;
	
	@GridLoggerResource
	protected GridLogger logger;

	/** */
	@GridInstanceResource
	protected Grid grid;

	/** */
	@GridLocalNodeIdResource
	protected UUID nodeId;
	
	@Override
	public Long reduce(List<GridComputeJobResult> jobResultList) throws GridException {
		Long result = 0L;
		for (GridComputeJobResult jresult : jobResultList) {
			result += (Long) jresult.getData();
		}
		logger.info("--> the compute result is:"+result);
		return result;
	}

	@SuppressWarnings("unchecked")
	public void loadCache(int args) throws GridException {
		GridCache<Integer, Integer[]> cache = grid.cache("share-data");
		cache.globalClearAll(0);
		for (int i = 0; i < args; i++) {
			try {
				cache.putx(i, new Integer[]{i,i+1});
				logger.info("--> gridcache load the key:"+i);
			} catch (GridException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected Collection<? extends GridComputeJob> split(int gridSize, Object arg) throws GridException {
		Collection<GridComputeJobAdapter> list = new ArrayList<GridComputeJobAdapter>();
		GridClassLoaderCache.printMemoryStats();
		int len = arg == null ? 1000 : Integer.parseInt(arg.toString());
		loadCache(len);
		GridClassLoaderCache.printMemoryStats();
		GridCache<Integer, Integer[]> cache = grid.cache("share-data");
		for (final GridCacheEntry<Integer, Integer[]> entry : cache.entrySet()) {
			list.add(new GridComputeJobAdapter() {
				
				private static final long serialVersionUID = 1L;

				@Override
				public Long execute() throws GridException {
					Long r = 0L;
					for (Integer pp : entry.getValue()) {
						r += pp;
					}
					logger.info("--> excute the job with key["+entry.getKey()+"] on the node:"+nodeId);
					return r;
				}
			});
		}
		return list;
	}
}
