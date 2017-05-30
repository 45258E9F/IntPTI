window.onload = initAll;

var _ace_editor;
// global variable stores which file is opened by editor
var _current_file;

function initAll() {
    initEditor();
    initFileTree();
}

function initFileTree() {
    $('#file_tree').fileTree({ root: 'fileTree', script: 'http://localhost:9026', expandSpeed: 1, collapseSpeed:1 }, function(file) {
        loadFile(file);
    });
}

function initEditor() {
    _ace_editor = ace.edit("editor");
    _ace_editor.renderer.setHScrollBarAlwaysVisible(false);
    _ace_editor.renderer.setShowGutter(true);
    _ace_editor.setTheme("ace/theme/xcode");
    _ace_editor.setShowPrintMargin(true);
    _ace_editor.setDisplayIndentGuides(true);
    _ace_editor.setHighlightSelectedWord(true);
    _ace_editor.setHighlightActiveLine(false);
    _ace_editor.setPrintMarginColumn(80);
    _ace_editor.setAnimatedScroll(true);
    // the editor should be read-only
    _ace_editor.setReadOnly(true);

    _ace_editor.session.setMode("ace/mode/c_cpp");
    _ace_editor.session.setValue("Choose a file to show its source.");
    _ace_editor.session.setTabSize(4);
    _ace_editor.session.setUseWrapMode(false);
}

function loadFile(file) {
    _ace_editor.session.clearAnnotations();
    if (file === _current_file) {
        return;
    }
    // load source code
    $.get('http://localhost:9026', { file: file }, function(content) {
       _current_file = file;
       _ace_editor.session.setValue(unescape(content));
    });
    // load fix list

}