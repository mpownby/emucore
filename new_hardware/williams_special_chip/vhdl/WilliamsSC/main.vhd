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
				AH : out std_logic_vector(15 downto 8);
				AL : inout std_logic_vector(7 downto 0);
				BA_BS_N : in std_logic;
				CS_N : in std_logic;
				D : inout std_logic_vector(7 downto 0);
				E_N : in std_logic;
				HALT_N : in std_logic;
				HTCF_N : out std_logic;
				MHZ4 : in std_logic;
				R_WN : inout std_logic;
				RESET_N : in std_logic;
				TCF_N : in std_logic;
				U_LN : in std_logic;
				WINH_N : out std_logic);
end main;

architecture Behavioral of main is

begin

AH(15 downto 8) <= "11111111";
AL(7 downto 0) <= "11111111";
D(7 downto 0) <= "11111111";
HTCF_N <= '1';
R_WN <= '1';
WINH_N <= '1';

end Behavioral;
