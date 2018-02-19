package eu.toop.mp.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This method is called by the <code>toop-interface</code> project in the
 * direction DP to DC.
 *
 * @author Philip Helger
 */
@WebServlet("/dpinput")
public class DPInputServlet extends HttpServlet {
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO
	}
}
