<%@ taglib uri="./Track" prefix="gts" %>
<%@ page isELIgnored="true" contentType="text/html; charset=UTF-8" %>
<%
//response.setContentType("text/html; charset=UTF-8");
//response.setCharacterEncoding("UTF-8");
response.setHeader("CACHE-CONTROL", "NO-CACHE");
response.setHeader("PRAGMA"       , "NO-CACHE");
response.setHeader("P3P"          , "CP='IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT'");
response.setDateHeader("EXPIRES"  , 0         );
%>
<!DOCTYPE html>
<html lang="en">
<gts:var ifKey="notDefined" value="true">
</gts:var>
<head>
    <gts:var>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">
    </gts:var>

    <link rel="icon" type="image/png" sizes="16x16" href="/favicon.png">
    <gts:var>
    <title>${pageTitle}</title>
    </gts:var>
    <link href="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/plugins/bower_components/sidebar-nav/dist/sidebar-nav.min.css" rel="stylesheet">
    <link href="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/css/animate.css" rel="stylesheet">
    <link href="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/css/style.css" rel="stylesheet">
    <link href="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/css/colors/blue.css" id="theme" rel="stylesheet">
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
      <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
      <![endif]-->
    <script src="https://www.w3schools.com/lib/w3data.js"></script>
    <gts:track section="javascript"/>
    <gts:track section="stylesheet"/>
</head>

<body onload="<gts:track section='body.onload'/>" onunload="<gts:track section='body.onunload'/>">
    <div id="wrapper">
        <!-- Top Navigation -->
        <nav class="navbar navbar-default navbar-static-top m-b-0">
            <div class="navbar-header">
                <!-- Toggle icon for mobile view -->
                <a class="navbar-toggle hidden-sm hidden-md hidden-lg " href="javascript:void(0)" data-toggle="collapse" data-target=".navbar-collapse"><i class="ti-menu"></i></a>
                <!-- Logo -->
                <div class="top-left-part">
                    <a class="logo" href="./?page=menu.top">
                        <!-- Logo icon image, you can use font-icon also -->
                        <img src="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/plugins/images/agileadmin-logo.png" alt="home" />

                        <span class="hidden-xs"><img src="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/plugins/images/agileadmin-text.png" alt="home" /></span>
                    </a>
                </div>
                <!-- /Logo -->
                <!-- Search input and Toggle icon -->
                <ul class="nav navbar-top-links navbar-left hidden-xs">
                    <li><a href="javascript:void(0)" class="open-close hidden-xs waves-effect waves-light"><i class="icon-arrow-left-circle ti-menu"></i></a></li>
                    <li>
                      <!-- search bar -->
                    </li>
                </ul>
                <!-- This is the message dropdown -->
                <ul class="nav navbar-top-links navbar-right pull-right">
                    <!-- .user dropdown -->
                    <li class="dropdown">
                        <a class="dropdown-toggle profile-pic" data-toggle="dropdown" href="#">${accountDesc} </a>
                        <ul class="dropdown-menu dropdown-user animated flipInY">
                            <li><a href="./?page=acct.info"><i class="ti-user"></i> &nbsp;My Account</a></li>
                            <li><a href="./?page=passwd"><i class="ti-settings"></i> &nbsp;My Password</a></li>
                            <li role="separator" class="divider"></li>
                            <li><a href="./?page=login"><i class="fa fa-power-off"></i> &nbsp;Logout</a></li>
                        </ul>
                        <!-- /.user dropdown-user -->
                    </li>
                    <!-- /.user dropdown -->
                    <li class="right-side-toggle"> <a class="waves-effect waves-light" href="javascript:void(0)"><i class="ti-settings"></i></a></li>
                    <!-- /.dropdown -->
                </ul>
            </div>
            <!-- /.navbar-header -->
            <!-- /.navbar-top-links -->
            <!-- /.navbar-static-side -->
        </nav>
        <!-- End Top Navigation -->
        <!-- Left navbar-header -->
        <div class="navbar-default sidebar" role="navigation">
            <div class="sidebar-nav navbar-collapse">
                <ul class="nav" id="side-menu">
                    <li class="sidebar-search hidden-sm hidden-md hidden-lg">
                        <!-- input-group -->
                        <div class="input-group custom-search-form">
                            <input type="text" class="form-control" placeholder="Search...">
                            <span class="input-group-btn">
                        <button class="btn btn-default" type="button"> <i class="fa fa-search"></i> </button>
                        </span>
                        </div>
                        <!-- /input-group -->
                    </li>
                    <li><a href="./?page=menu.top" class="waves-effect"><i class="linea-icon linea-basic fa-fw" data-icon="v"></i> <span class="hide-menu"><i class="fa fa-briefcase"></i> &nbsp;Dashboard</span></a></li>
                    <li>
                        <a href="javascript:void(0)" class="waves-effect"><i data-icon="F" class="linea-icon linea-software fa-fw"></i> <span class="hide-menu"><i class="fa fa-map-marker"></i> &nbsp;Mapping<span class="fa arrow"></span></span></a>
                        <ul class="nav nav-second-level">
                            <li> <a href="./?page=map.device">Vehicle Map</a> </li>
                            <li> <a href="./?page=map.fleet">Fleet Map</a> </li>
                        </ul>
                    </li>

                    <li>
                        <a href="javascript:void(0)" class="waves-effect"><i data-icon="F" class="linea-icon linea-software fa-fw"></i> <span class="hide-menu"><i class="fa fa-bar-chart"></i> &nbsp;Reports<span class="fa arrow"></span></span></a>
                        <ul class="nav nav-second-level">
                            <li> <a href="javascript:void(0)">Device Detail</a> </li>
                            <li> <a href="javascript:void(0)">Group Detail</a> </li>
                            <li> <a href="javascript:void(0)">Group Summary</a> </li>
                            <li> <a href="javascript:void(0)">Device Performance</a> </li>
                            <li> <a href="javascript:void(0)">Operator Performance</a> </li>
                        </ul>
                    </li>

                    <li>
                        <a href="javascript:void(0)" class="waves-effect"><i data-icon="F" class="linea-icon linea-software fa-fw"></i> <span class="hide-menu"><i class="fa fa-power-off"></i> &nbsp; Manage<span class="fa arrow"></span></span></a>
                        <ul class="nav nav-second-level">
                            <li> <a href="javascript:void(0)">Users</a> </li>
                            <li> <a href="javascript:void(0)">Devices</a> </li>
                            <li> <a href="javascript:void(0)">Fleets</a> </li>
                            <li> <a href="javascript:void(0)">Geozones</a> </li>
                        </ul>
                    </li>

                    <li>
                        <a href="javascript:void(0)" class="waves-effect"><i data-icon="F" class="linea-icon linea-software fa-fw"></i> <span class="hide-menu"><i class="fa fa-bolt"></i> &nbsp;System<span class="fa arrow"></span></span></a>
                        <ul class="nav nav-second-level">
                            <li> <a href="javascript:void(0)">Accounts</a> </li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
        <!-- Left navbar-header end -->
        <!-- Page Content -->
        <div id="page-wrapper">
            <div class="container-fluid">
                <div class="row" style="margin-top: 20px;">
                    <div class="col-md-12">
                        <div class="white-box">
                            <gts:track section="content.body"/>
                        </div>
                    </div>
                </div>
                <!-- .row -->
            </div>
            <!-- /.container-fluid -->
            <footer class="footer text-center"> ${copyright} </footer>
        </div>
        <!-- /#page-wrapper -->
    </div>
    <!-- /#wrapper -->
    <!-- jQuery -->
    <script src="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/plugins/bower_components/jquery/dist/jquery.min.js"></script>
    <script src="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/plugins/bower_components/sidebar-nav/dist/sidebar-nav.min.js"></script>
    <script src="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/js/jquery.slimscroll.js"></script>
    <script src="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/js/waves.js"></script>
    <script src="https://gbpawebdiag949.blob.core.windows.net/gbpa-web/opengts/agileadmin/js/custom.min.js"></script>
</body>
</html>
