/* 
 * services-panel.js
*/

Ext.onReady(function(){
    Ext.QuickTips.init();
    Ext.BLANK_IMAGE_URL = 'images/default/s.gif';


    var reader= new Ext.data.XmlReader(
       {record: 'entry'},
       ['title', 'author', 
		{name:'pubDate', type:'date'}, 
		{name: 'link', mapping: 'link@href'}, 
		'summary', 
		'content'
	   ]
    );

    // row expander 
    var expander = new Ext.ux.grid.RowExpander({
        tpl : new Ext.Template(
            '<p><b>Usage: </b> {summary}</p>'
            //'<p><b>Info: </b> {content}</p>'
        )
    });

    //load the data, default sorting  ascending
    store = new Ext.data.Store ({
        url: '/opal2/opalServices.xml',
        reader: reader
    });
	store.setDefaultSort('title', 'asc'); 
    store.load();
    
    //display the main with the data grid
    var grid = new Ext.grid.GridPanel({
        store: store,
        cm: new Ext.grid.ColumnModel({
            defaults: {
                width: 40,
                sortable: true
            },
            columns: [
                expander,
                {id:'title', header:"   Service submission form", dataIndex:'title', renderer:formatTitle, width:430},
                {header: "WSDL URL for programmatic access", dataIndex: 'link', renderer: formatURL, width: 430}
            ]
        }),
        viewConfig: {
            forceFit:true
        },        
		columnLines: true,
        width: 900,
        autoHeight: true,
        plugins: expander,
        collapsible: true,
        animCollapse: false,
        //title: 'Available Services',
		iconCls: 'icon-grid'
        //renderTo: document.body
    });

    //format title
    function formatTitle (value, p, record) {
        hostName = "<%=tomcatUrl%>";
        submissionFormLink = hostName + "/opal2/CreateSubmissionForm.do?serviceURL=" + "<%=opalUrl%>" + "%2F"
        URLarray = record.data.link.split("/");
        serviceName = URLarray[URLarray.length - 1];
        submissionFormLink = submissionFormLink + serviceName;
        return String.format ('<div class="serviceName"><a href="{1}">{0}</a></div>',
                value, submissionFormLink);
    }

    //Format Web Service URL
    function formatURL(value, p, record) {
        return String.format(
            '<div class="serviceLink"><a href="{0}">{0}</a></div>', record.data.link);
    }

    //Format Web Service Docs
    function formatDoc(value, p, record) {
        return String.format(
            '<span class="serviceDescription">{0}</span>', record.data.summary );
    }

    function filterTree(e){
        var text = e.target.value;
        store.filter("summary", text, true, false);
    };

    var searchBox =  new Ext.form.TextField({
        width: 200,
        emptyText:'  Type full or partial service name',
        listeners:{
            render: function(f) {f.el.on('keydown', filterTree, f, {buffer: 300});}
        }
    });


    var tb = new Ext.Toolbar();
//    tb.add('-', { icon: 'images/add/icon-question.gif', // icons can also be specified inline
//			      cls: 'x-btn-icon',
//				  tooltip: '<b>Quick Tips</b><br/>Icon only button with tooltip'
//			}, '-');
      ttText = '<b>Sort</b> table columns by clicking on the column header<br>' + 
	           '<b>Submit jobs:</b> click on a service name in the Service submission form column<br>'+
               '<b>Info</b> click on an icon near the service name to see more application info';

	  tb.add ('-', {text:'Search'}, '-', searchBox, '-');
      tb.add ({
	        width:190,
            text:'                Help ',
            tooltip: ttText,
            //iconCls:'question'
        }, '-');

    var panel = new Ext.Panel ({
        autoScroll:true,
        width:900,
        autoHeight: true,
        layout:'fit',
        items: grid,
		tbar: tb
    });

    panel.render('feed-viewer');
});
