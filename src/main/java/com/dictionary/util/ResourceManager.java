package com.dictionary.util;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.ref.WeakReference;

public class ResourceManager {
    private static final ResourceManager INSTANCE = new ResourceManager();
    private final Set<WeakReference<ExecutorService>> executors = new HashSet<>();
    private final Set<WeakReference<Image>> images = new HashSet<>();
    private final Set<WeakReference<Node>> nodes = new HashSet<>();
    private final Map<String, WeakReference<Object>> resources = new HashMap<>();
    
    private ResourceManager() {}
    
    public static ResourceManager getInstance() {
        return INSTANCE;
    }
    
    // Register executor service for cleanup
    public void registerExecutor(ExecutorService executor) {
        if (executor != null) {
            executors.add(new WeakReference<>(executor));
        }
    }
    
    // Register image for cleanup
    public void registerImage(Image image) {
        if (image != null) {
            images.add(new WeakReference<>(image));
        }
    }
    
    // Register node for cleanup
    public void registerNode(Node node) {
        if (node != null) {
            nodes.add(new WeakReference<>(node));
        }
    }
    
    // Register generic resource
    public void registerResource(String key, Object resource) {
        if (resource != null) {
            resources.put(key, new WeakReference<>(resource));
        }
    }
    
    // Clean up all resources
    public void cleanup() {
        // Clean up executors
        cleanupExecutors();
        
        // Clean up images
        cleanupImages();
        
        // Clean up nodes
        cleanupNodes();
        
        // Clean up generic resources
        cleanupResources();
        
        // Request garbage collection
        System.gc();
    }
    
    // Clean up specific executor
    public void cleanupExecutor(ExecutorService executor) {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // Clean up all executors
    private void cleanupExecutors() {
        Iterator<WeakReference<ExecutorService>> it = executors.iterator();
        while (it.hasNext()) {
            ExecutorService executor = it.next().get();
            if (executor != null) {
                cleanupExecutor(executor);
            }
            it.remove();
        }
    }
    
    // Clean up images
    private void cleanupImages() {
        Iterator<WeakReference<Image>> it = images.iterator();
        while (it.hasNext()) {
            Image image = it.next().get();
            if (image != null) {
                image.cancel();
            }
            it.remove();
        }
    }
    
    // Clean up nodes
    private void cleanupNodes() {
        Iterator<WeakReference<Node>> it = nodes.iterator();
        while (it.hasNext()) {
            Node node = it.next().get();
            if (node != null) {
                // Remove event handlers
                node.setOnMouseClicked(null);
                node.setOnKeyPressed(null);
                node.setOnKeyReleased(null);
                
                // Clean up specific node types
                if (node instanceof ImageView) {
                    ((ImageView) node).setImage(null);
                } else if (node instanceof ListView) {
                    ((ListView<?>) node).setItems(null);
                } else if (node instanceof TableView) {
                    ((TableView<?>) node).setItems(null);
                }
                
                // Remove from parent
                if (node.getParent() != null) {
                    node.getParent().getChildrenUnmodifiable().remove(node);
                }
            }
            it.remove();
        }
    }
    
    // Clean up generic resources
    private void cleanupResources() {
        Iterator<Map.Entry<String, WeakReference<Object>>> it = resources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, WeakReference<Object>> entry = it.next();
            Object resource = entry.getValue().get();
            if (resource != null) {
                if (resource instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) resource).close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            it.remove();
        }
    }
    
    // Clean up specific resource type
    public <T> void cleanupResource(T resource, ResourceCleanupHandler<T> handler) {
        if (resource != null && handler != null) {
            handler.cleanup(resource);
        }
    }
    
    // Resource cleanup handler interface
    public interface ResourceCleanupHandler<T> {
        void cleanup(T resource);
    }
    
    // Check if a resource is still valid
    public boolean isResourceValid(String key) {
        WeakReference<Object> ref = resources.get(key);
        return ref != null && ref.get() != null;
    }
    
    // Get resource if still valid
    @SuppressWarnings("unchecked")
    public <T> T getResource(String key) {
        WeakReference<Object> ref = resources.get(key);
        return ref != null ? (T) ref.get() : null;
    }
    
    // Remove invalid references
    public void removeInvalidReferences() {
        // Clean up executors
        executors.removeIf(ref -> ref.get() == null);
        
        // Clean up images
        images.removeIf(ref -> ref.get() == null);
        
        // Clean up nodes
        nodes.removeIf(ref -> ref.get() == null);
        
        // Clean up resources
        resources.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }
} 