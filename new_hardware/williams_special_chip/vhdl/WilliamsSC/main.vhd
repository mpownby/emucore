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
				CLK_4MHZ : in std_logic;
				R_WN : inout std_logic;
				RESET_N : in std_logic;
				TCF_N : in std_logic;
				U_LN : in std_logic;
				WINH_N : out std_logic);
end main;

architecture Behavioral of main is

--mode bits
signal srcstride : std_logic;
signal dststride : std_logic;
signal synce : std_logic;
signal transparency : std_logic;
signal solid : std_logic;
signal shift : std_logic;
signal inhl : std_logic;
signal inhu : std_logic;
 
--registers
signal color : std_logic_vector(7 downto 0);
signal src : std_logic_vector(15 downto 0);
signal dst : std_logic_vector(15 downto 0);
signal width : std_logic_vector(7 downto 0);
signal height : std_logic_vector(7 downto 0);
 
signal reqhalt : std_logic := '0';
signal doblit : std_logic := '0';
signal csqueued : std_logic := '0';

begin

AH(15 downto 8) <= "11111111";
--AL(7 downto 0) <= "11111111";
--D(7 downto 0) <= "11111111";
HTCF_N <= '1';
--R_WN <= '1';
WINH_N <= '1';

watch_cs : process(E_N,RESET_N)
	begin
		if (RESET_N='0') then
			-- todo
		else
			if (falling_edge(E_N)) then
				if (CS_N='0') then
					csqueued <= '1';
				end if;
			end if;
		end if;
	end process;

loadreg: process(CLK_4MHZ,RESET_N)
	begin
		if(RESET_N='0') then
			doblit<='0';
		else
			if(rising_edge(CLK_4MHZ)) then
				if (csqueued = '1') then
					case AL(2 downto 0) IS
						when "000" =>
							--set mode bits
							srcstride <= D(0);
							dststride <= D(1);
							synce <= D(2);
							transparency <= D(3);
							solid <= D(4);
							shift <= D(5);
							inhl <= D(6);
							inhu <= D(7);
                                        
							--start blit
							doblit <= '1';
                                        
						when "001" =>
							--set color replacement
							color <= D;
						when "010" =>
							--set src high
							src(15 downto 8) <= D;
						when "011" =>
							--set src low
							src(7 downto 0) <= D;
						when "100" =>
							--set dest high
							dst(15 downto 8) <= D;
						when "101" =>
							--set dest low
							dst(7 downto 0) <= D;
						when "110" =>
							--set width
							width <= D xor "00000100";
						when "111" =>
							--set height
							height <= D xor "00000100";
						when others =>
						end case;

						-- wait for CS to be lowered again
						csqueued <= '0';
						
				end if; -- CS was lowered
			end if; -- rising edge of CLK_4MHZ
		end if;	-- else if RESET_N is not low
	end process;

end Behavioral;
