// Copyright (C) 2012 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.plugins;


import com.google.gerrit.extensions.annotations.Export;
import com.google.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

import com.google.inject.Inject;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.reviewdb.client.Project;

@Export("/matrix")
@Singleton

public class MatrixACL extends HttpServlet {
    private static final long serialVersionUID = -891739817389L;

    private final ProjectCache projectCache;


    @Inject
    protected MatrixACL(ProjectCache projectCache ) {
        this.projectCache = projectCache;
        this.roles      = new ArrayList<Project.NameKey>();
        this.generated  = new ArrayList<Project.NameKey>();
        this.projects   = new ArrayList<Project.NameKey>();
    }

    private final List<Project.NameKey> roles;
    private final List<Project.NameKey> generated;
    private final List<Project.NameKey> projects;


    private void update()
    {
        roles.clear();
        generated.clear();
        projects.clear();

        Project.NameKey nk = Project.NameKey.parse("MatrixRoles");
        Project.NameKey gk = Project.NameKey.parse("MatrixApplied");
        Project.NameKey mk = Project.NameKey.parse("All-Projects");

        for (Project.NameKey st : this.projectCache.all()) {

            ProjectState projectS  = projectCache.get(st);
            Project      project   = projectS.getProject();
            Project.NameKey parent = project.getParent();

            if (parent != null ) {
                if (parent.equals(nk)) {
                    this.roles.add(st);
                } else if (parent.equals(gk)) {
                    this.generated.add(st);
                }
            // TODO weird thing to get all the top level projects. do we really want to?
            } else if ((!st.equals(nk)) && (!st.equals(gk)) && (!st.equals(mk))) {
                this.projects.add(st);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    resp.setContentType("text/html");
    resp.setCharacterEncoding("UTF-8");


    PrintWriter out = resp.getWriter();


    try {
        update();

        out.write(
                "<style type='text/css'>\n" +
                " #wopwop tr:nth-child(2n) { background: #eee; }" +
                "</style>\n"
                );
        out.write("<form><table id='wopwop'><tr><th></th>\n");

        for (Project.NameKey g  : this.roles) {
            out.write("<th>" +   g.get() + "</th>");
        }

        out.write("</tr>\n");

        out.write("<h1> Matrix-ACLs </h1>");

        for (Project.NameKey g  : this.projects) {
            out.write("<tr><td>" + g.get() +  "</td>");

            for (Project.NameKey e  : this.roles) {
                out.write("<td> <label> <input type='checkbox'> " + e.get() +   "  </label> </td>");
            }
            out.write("</tr>");
        }


        out.write("</table><br><input type='submit'></form>");

    } finally {
        out.close();
    }
    }
}
