window.onload = initAll;

function initAll() {
    initFileTree();
}

function initFileTree() {
    $('#file_tree').fileTree({ root: 'fileTree', script: 'http://localhost:9026', expandSpeed: 1, collapseSpeed:1 }, function(file) {
        alert(file);
    });
}
