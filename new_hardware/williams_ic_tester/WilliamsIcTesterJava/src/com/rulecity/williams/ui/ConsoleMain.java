package com.rulecity.williams.ui;

import com.rulecity.williams.biz.Crc;
import com.rulecity.williams.biz.listener;
import com.rulecity.williams.interfaces.ILogger;

/**
 * Created by Matt on 8/30/2014.
 */
public class ConsoleMain implements ILogger
{
	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.out.println("Arguments: [serial device such as COM1] [path to firmware binary]");
			return;
		}

		ConsoleMain myself = new ConsoleMain();

		listener instance = new listener(myself, args[1]);
		try
		{
			instance.init(args[0]);

			// go until user aborts us (TODO, make a way to be aborted! hehe)
			for (;;)
			{
				instance.think();

				// don't hog the CPU!
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ex)
				{
					Thread.currentThread().interrupt();
				}
			}

			//instance.shutdown();
		}
		catch (Exception ex)
		{
			System.err.println(ex.getMessage());
		}
	}

	@Override
	public void Log(String strText)
	{
		System.out.println(strText);
	}

	@Override
	public void LogError(String strText)
	{
		System.err.println(strText);
	}
}
