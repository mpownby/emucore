----------------------------------------------------------------------------------
-- Company: 
-- Engineer: 
-- 
-- Create Date:    20:19:40 08/05/2014 
-- Design Name: 
-- Module Name:    main - Behavioral 
-- Project Name: 
-- Target Devices: 
-- Tool versions: 
-- Description: 
--
-- Dependencies: 
--
-- Revision: 
-- Revision 0.01 - File Created
-- Additional Comments: 
--
----------------------------------------------------------------------------------
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

-- Uncomment the following library declaration if using
-- arithmetic functions with Signed or Unsigned values
--use IEEE.NUMERIC_STD.ALL;

-- Uncomment the following library declaration if instantiating
-- any Xilinx primitives in this code.
--library UNISIM;
--use UNISIM.VComponents.all;

entity main is
    Port (
				A : inout  STD_LOGIC_VECTOR(15 downto 14);
           B : inout  STD_LOGIC_VECTOR(15 downto 0);
           C : inout  STD_LOGIC_VECTOR(15 downto 0));
end main;

architecture Behavioral of main is

begin

A(15 downto 14) <= "11";
B <= "1111111111111111";
C <= "1111111111111111";

end Behavioral;
