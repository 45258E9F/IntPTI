window.onload = initAll;

var _ace_editor;
// global variable stores which file is opened by editor
var _current_file;
// the tab size for the editor
var _tab_size = 4;
// historic markers are stored for removal
var _marker_set = [];
// we use range to mark code in the editor
var Range = ace.require('ace/range').Range;

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
    _ace_editor.session.setTabSize(_tab_size);
    _ace_editor.session.setUseWrapMode(false);
}

function loadFile(file) {
    _ace_editor.session.clearAnnotations();
    if (file === _current_file) {
        return;
    }
    // remove the existing markers
    removeMarker();
    // cache the selected fixes for this file
    if (typeof _current_file !== 'undefined' && $('#mode_selected').text() === 'Manual') {
        var selected = [];
        $('.circular.button.active').parent().parent().each(function () {
            selected.push(this.id);
        });
        $.post("http://localhost:9026", { op: 'cache', file: file, list: String(selected) });
    }
    // load source code
    $.get("http://localhost:9026", { file: file }, function(content) {
       _current_file = file;
       _ace_editor.session.setValue(unescape(content));
    });
    // load fix list
    $.post("http://localhost:9026", { dir: 'fixList', file: file }).done(function (content) {
        var l = $('#fix_list');
        l.find('*').remove();
        l.append(content);
        $('.item.intfix').click(drawMarker);
        $('.circular.ui.button').on('click', function(event) {
            event.stopPropagation();
            var item, indent, new_indent, button;
            if ($(this).hasClass('active') && $('#mode_selected').text() === 'Manual') {
                $(this).removeClass('active');
                // we also remove the active status of depending fixes
                item = $(this).parent().parent();
                indent = Number($(item).attr("data-indent"));
                if (indent > 0) {
                    item = $(item).prev();
                    while ($(item).hasClass("item intfix")) {
                        new_indent = Number($(item).attr("data-indent"));
                        if (new_indent < indent) {
                            indent = new_indent;
                            button = $(item).children().children('button');
                            $(button).removeClass('active');
                        }
                        if (indent === 0) {
                            break;
                        }
                        item = $(item).prev();
                    }
                }
            } else {
                $(this).addClass('active');
                // we also set the depended fixes as active
                item = $(this).parent().parent();
                indent = Number($(item).attr("data-indent"));
                item = $(item).next();
                while ($(item).hasClass("item intfix")) {
                    new_indent = Number($(item).attr("data-indent"));
                    if (new_indent > indent) {
                        button = $(item).children().children('button');
                        $(button).addClass('active');
                    } else {
                        break;
                    }
                    item = $(item).next();
                }
            }
        });
    });
}

function drawMarker() {
    var fix_id = $(this).attr('id');
    $.post("http://localhost:9026", { dir: 'fixDraw', id: fix_id }).done(function (content) {
        // the response is in JSON format
        var info_json = JSON.parse(content);
        var start_line = info_json.startLine;
        var end_line = info_json.endLine;
        var start_offset = info_json.startOffset;
        var end_offset = info_json.endOffset;
        // remove old ranges before drawing the new range
        removeMarker();
        // draw ranges in the editor
        var marker = _ace_editor.session.addMarker(new Range(start_line - 1, start_offset, end_line - 1, end_offset), "ace-code-highlight", "line", false);
        _ace_editor.scrollToLine(start_line - 1, true, true);
        _marker_set.push(marker);
    });
}

function removeMarker() {
    for (var i in _marker_set) {
        _ace_editor.session.removeMarker(_marker_set[i]);
    }
    _marker_set = [];
}

function endSession() {
    var chosen_mode = $('#mode_selected').text();
    if (chosen_mode === 'Mode') {
        if (!window.confirm("No mode is selected. The default is to apply all generated fixes. \nAre you sure?")) {
            return;
        }
    }
    if (chosen_mode === 'Manual') {
        // cache the current selected fixes in the list
        if (typeof _current_file !== 'undefined') {
            var selected = [];
            $('.circular.button.active').parent().parent().each(function () {
                selected.push(this.id);
            });
            $.post("http://localhost:9026", { op: 'cache', file: _current_file, list: String(selected) });
        }
    }
    // end the server session and then close the browser window
    $.post("http://localhost:9026", { op: 'close' });
}